package hu.javagpt.server;

import com.google.gson.Gson;
import hu.javagpt.common.Message;
import hu.javagpt.model.Chat;
import hu.javagpt.model.User;

import java.io.*;
import java.net.Socket;
import java.util.List;

/**
 * Egy kliens kapcsolatot kezel egy külön szálban.
 * Beolvassa a kliens JSON üzeneteit, feldolgozza őket, és visszaküldi a választ.
 * Szükség esetén az összes bejelentkezett kapcsolatot értesíti.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ServerState state;
    private final PrintWriter out;
    private final BufferedReader in;
    private final Gson gson = new Gson();
    private final OllamaService ollamaService = new OllamaService();

    private volatile String loggedInUser = null;

    /** A legutóbb elküldött prompt chat azonosítója (megszakításhoz). */
    private volatile String pendingChatId = null;

    /** A legutóbb elküldött prompt szövege (megszakításhoz). */
    private volatile String pendingPrompt = null;

    /**
     * @param socket a kliens sockete
     * @param state  a szerver megosztott állapota
     * @throws IOException ha a stream-ek megnyitása sikertelen
     */
    public ClientHandler(Socket socket, ServerState state) throws IOException {
        this.socket = socket;
        this.state = state;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /**
     * @return a bejelentkezett felhasználó neve, vagy null ha nem jelentkezett be
     */
    public String getLoggedInUser() {
        return loggedInUser;
    }

    /**
     * JSON sor küldése a kliensnek. Synchronized, hogy több szálból is biztonságos legyen.
     *
     * @param json a küldendő JSON sor
     */
    public synchronized void send(String json) {
        out.println(json);
    }

    private void sendMsg(Message msg) {
        send(gson.toJson(msg));
    }

    @Override
    public void run() {
        state.addClient(this);
        try {
            String line;
            while ((line = in.readLine()) != null) {
                handle(gson.fromJson(line, Message.class));
            }
        } catch (IOException e) {
            // kapcsolat megszakadt
        } finally {
            ollamaService.cancel();
            state.removeClient(this);
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private void handle(Message msg) {
        try {
            switch (msg.type) {
                case "REGISTER"    -> handleRegister(msg);
                case "LOGIN"       -> handleLogin(msg);
                case "LOGOUT"      -> handleLogout();
                case "NEW_CHAT"    -> handleNewChat();
                case "DELETE_CHAT" -> handleDeleteChat(msg);
                case "SEND_PROMPT" -> handleSendPrompt(msg);
                case "CANCEL"      -> handleCancel();
            }
        } catch (Exception e) {
            Message err = new Message();
            err.type = "SERVER_ERROR";
            sendMsg(err);
            loggedInUser = null;
        }
    }

    private void handleRegister(Message msg) throws IOException {
        List<User> users = state.loadUsers();
        boolean exists = users.stream().anyMatch(u -> u.getUsername().equals(msg.username));
        Message resp = new Message();
        if (exists) {
            resp.type = "REGISTER_ERROR";
            resp.message = "Ez a felhasználónév foglalt!";
        } else {
            users.add(new User(msg.username, msg.password));
            state.saveUsers(users);
            resp.type = "REGISTER_OK";
        }
        sendMsg(resp);
    }

    private void handleLogin(Message msg) throws IOException {
        List<User> users = state.loadUsers();
        boolean ok = users.stream().anyMatch(
                u -> u.getUsername().equals(msg.username) && u.getPassword().equals(msg.password)
        );
        Message resp = new Message();
        if (ok) {
            loggedInUser = msg.username;
            resp.type = "LOGIN_OK";
            sendMsg(resp);
            sendChatsUpdateToSelf();
        } else {
            resp.type = "LOGIN_ERROR";
            resp.message = "Sikertelen bejelentkezés";
            sendMsg(resp);
        }
    }

    private void handleLogout() {
        loggedInUser = null;
        Message resp = new Message();
        resp.type = "LOGOUT_OK";
        sendMsg(resp);
    }

    private void handleNewChat() throws IOException {
        if (loggedInUser == null) return;
        List<Chat> chats = state.loadChats();
        long maxNum = chats.stream()
                .filter(c -> loggedInUser.equals(c.getUsername()))
                .map(Chat::getTitle)
                .filter(t -> t.startsWith("Csevegés "))
                .mapToLong(t -> {
                    try { return Long.parseLong(t.substring("Csevegés ".length())); }
                    catch (NumberFormatException e) { return 0L; }
                })
                .max().orElse(0L);
        Chat chat = new Chat(loggedInUser, "Csevegés " + (maxNum + 1));
        chats.add(chat);
        state.saveChats(chats);
        state.broadcastChatsUpdateForUser(loggedInUser);
    }

    private void handleDeleteChat(Message msg) throws IOException {
        if (loggedInUser == null) return;
        List<Chat> chats = state.loadChats();
        chats.removeIf(c -> msg.chatId.equals(c.getId()) && loggedInUser.equals(c.getUsername()));
        state.saveChats(chats);
        state.broadcastChatsUpdateForUser(loggedInUser);
    }

    private void handleSendPrompt(Message msg) throws IOException {
        if (loggedInUser == null) return;

        List<Chat> chats = state.loadChats();
        Chat chat = chats.stream()
                .filter(c -> msg.chatId.equals(c.getId()))
                .findFirst().orElse(null);
        if (chat == null) return;

        chat.getMessages().add(loggedInUser + ": " + msg.prompt);
        state.saveChats(chats);

        pendingChatId = msg.chatId;
        pendingPrompt = msg.prompt;

        state.broadcastChatsUpdateForUser(loggedInUser);

        String chatId = msg.chatId;
        String username = loggedInUser;
        StringBuilder aiBuffer = new StringBuilder();

        Message startMsg = new Message();
        startMsg.type = "AI_START";
        startMsg.chatId = chatId;
        state.broadcastToUser(username, gson.toJson(startMsg));

        ollamaService.generate(msg.prompt,
                chunk -> {
                    aiBuffer.append(chunk);
                    Message chunkMsg = new Message();
                    chunkMsg.type = "AI_CHUNK";
                    chunkMsg.chunk = chunk;
                    send(gson.toJson(chunkMsg));
                },
                () -> {
                    try {
                        List<Chat> cs = state.loadChats();
                        Chat c = cs.stream()
                                .filter(ch -> chatId.equals(ch.getId()))
                                .findFirst().orElse(null);
                        if (c != null) {
                            c.getMessages().add("AI: " + aiBuffer);
                            state.saveChats(cs);
                        }
                        state.broadcastChatsUpdateForUser(username);
                    } catch (IOException e) {
                        Message errMsg = new Message();
                        errMsg.type = "SERVER_ERROR";
                        send(gson.toJson(errMsg));
                        loggedInUser = null;
                        return;
                    }
                    Message doneMsg = new Message();
                    doneMsg.type = "AI_DONE";
                    state.broadcastToUser(username, gson.toJson(doneMsg));
                    pendingChatId = null;
                    pendingPrompt = null;
                },
                ex -> {
                    try {
                        List<Chat> cs = state.loadChats();
                        Chat c = cs.stream()
                                .filter(ch -> chatId.equals(ch.getId()))
                                .findFirst().orElse(null);
                        if (c != null) {
                            c.getMessages().add("Hiba.");
                            state.saveChats(cs);
                        }
                        state.broadcastChatsUpdateForUser(username);
                    } catch (IOException e) {
                        Message errMsg = new Message();
                        errMsg.type = "SERVER_ERROR";
                        send(gson.toJson(errMsg));
                        loggedInUser = null;
                        return;
                    }
                    Message errMsg = new Message();
                    errMsg.type = "AI_ERROR";
                    state.broadcastToUser(username, gson.toJson(errMsg));
                    pendingChatId = null;
                    pendingPrompt = null;
                }
        );
    }

    private void handleCancel() throws IOException {
        ollamaService.cancel();
        if (pendingChatId == null || loggedInUser == null) return;

        List<Chat> chats = state.loadChats();
        Chat chat = chats.stream()
                .filter(c -> pendingChatId.equals(c.getId()))
                .findFirst().orElse(null);

        if (chat != null && !chat.getMessages().isEmpty()) {
            String expected = loggedInUser + ": " + pendingPrompt;
            String last = chat.getMessages().get(chat.getMessages().size() - 1);
            if (expected.equals(last)) {
                chat.getMessages().remove(chat.getMessages().size() - 1);
                state.saveChats(chats);
            }
        }

        pendingChatId = null;
        pendingPrompt = null;
        state.broadcastChatsUpdateForUser(loggedInUser);

        Message doneMsg = new Message();
        doneMsg.type = "AI_DONE";
        state.broadcastToUser(loggedInUser, gson.toJson(doneMsg));
    }

    private void sendChatsUpdateToSelf() throws IOException {
        List<Chat> userChats = state.getUserChats(loggedInUser);
        Message msg = new Message();
        msg.type = "CHATS_UPDATE";
        msg.chats = userChats;
        sendMsg(msg);
    }
}
