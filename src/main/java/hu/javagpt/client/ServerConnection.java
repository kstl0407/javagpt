package hu.javagpt.client;

import com.google.gson.Gson;
import hu.javagpt.common.Message;
import hu.javagpt.model.Chat;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;

/**
 * A kliens oldali szerver kapcsolatot kezeli.
 *
 * Felépít egy TCP socket kapcsolatot a szerverhez, majd elindít egy háttérszálat
 * (daemon thread), amely folyamatosan olvassa a szerver üzeneteit.
 * Minden bejövő üzenetet a JavaFX Application Thread-en dolgoz fel
 * Platform.runLater segítségével.
 *
 * Az UI komponensek callback-eket regisztrálnak a különböző eseményekre,
 * és a küldő metódusokkal kommunikálnak a szerverrel.
 */
public class ServerConnection {

    private PrintWriter out;
    private final Gson gson = new Gson();

    private Consumer<String> onLoginResult;
    private Consumer<String> onRegisterResult;
    private Consumer<List<Chat>> onChatsUpdate;
    private Consumer<String> onAiStart;
    private Consumer<String> onAiChunk;
    private Runnable onAiDone;
    private Consumer<String> onAiError;
    private Runnable onServerError;

    /**
     * Kapcsolódás a szerverhez.
     *
     * @param host a szerver hosztneve vagy IP-je
     * @param port a szerver portszáma
     * @throws IOException ha a kapcsolódás sikertelen
     */
    public void connect(String host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        startReaderThread(in);
    }

    private void startReaderThread(BufferedReader in) {
        Thread readerThread = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    final Message msg = gson.fromJson(line, Message.class);
                    Platform.runLater(() -> handleIncoming(msg));
                }
            } catch (IOException e) {
                Platform.runLater(() -> {
                    if (onServerError != null) onServerError.run();
                });
            }
        }, "server-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void handleIncoming(Message msg) {
        switch (msg.type) {
            case "REGISTER_OK" -> {
                if (onRegisterResult != null) {
                    Consumer<String> cb = onRegisterResult;
                    onRegisterResult = null;
                    cb.accept("OK");
                }
            }
            case "REGISTER_ERROR" -> {
                if (onRegisterResult != null) {
                    Consumer<String> cb = onRegisterResult;
                    onRegisterResult = null;
                    cb.accept(msg.message);
                }
            }
            case "LOGIN_OK" -> {
                if (onLoginResult != null) {
                    Consumer<String> cb = onLoginResult;
                    onLoginResult = null;
                    cb.accept("OK");
                }
            }
            case "LOGIN_ERROR" -> {
                if (onLoginResult != null) {
                    Consumer<String> cb = onLoginResult;
                    onLoginResult = null;
                    cb.accept(msg.message);
                }
            }
            case "CHATS_UPDATE" -> {
                if (onChatsUpdate != null) onChatsUpdate.accept(msg.chats);
            }
            case "AI_START" -> {
                if (onAiStart != null) onAiStart.accept(msg.chatId);
            }
            case "AI_CHUNK" -> {
                if (onAiChunk != null) onAiChunk.accept(msg.chunk);
            }
            case "AI_DONE" -> {
                if (onAiDone != null) onAiDone.run();
            }
            case "AI_ERROR" -> {
                if (onAiError != null) onAiError.accept(msg.message);
            }
            case "SERVER_ERROR" -> {
                if (onServerError != null) onServerError.run();
            }
        }
    }

    /**
     * Regisztrációs kérelem küldése.
     *
     * @param username  kívánt felhasználónév
     * @param password  jelszó
     * @param callback  "OK" sikeres regisztrációnál, hibaüzenet különben
     */
    public void sendRegister(String username, String password, Consumer<String> callback) {
        onRegisterResult = callback;
        Message msg = new Message();
        msg.type = "REGISTER";
        msg.username = username;
        msg.password = password;
        send(msg);
    }

    /**
     * Bejelentkezési kérelem küldése.
     *
     * @param username  felhasználónév
     * @param password  jelszó
     * @param callback  "OK" sikeres bejelentkezésnél, hibaüzenet különben
     */
    public void sendLogin(String username, String password, Consumer<String> callback) {
        onLoginResult = callback;
        Message msg = new Message();
        msg.type = "LOGIN";
        msg.username = username;
        msg.password = password;
        send(msg);
    }

    /** Kijelentkezési kérelem küldése. */
    public void sendLogout() {
        Message msg = new Message();
        msg.type = "LOGOUT";
        send(msg);
    }

    /** Új csevegés létrehozásának kérése. */
    public void sendNewChat() {
        Message msg = new Message();
        msg.type = "NEW_CHAT";
        send(msg);
    }

    /**
     * Csevegés törlésének kérése.
     *
     * @param chatId a törlendő csevegés azonosítója
     */
    public void sendDeleteChat(String chatId) {
        Message msg = new Message();
        msg.type = "DELETE_CHAT";
        msg.chatId = chatId;
        send(msg);
    }

    /**
     * Prompt elküldése a szervernek AI generáláshoz.
     *
     * @param chatId a csevegés azonosítója
     * @param prompt a felhasználó üzenete
     */
    public void sendPrompt(String chatId, String prompt) {
        Message msg = new Message();
        msg.type = "SEND_PROMPT";
        msg.chatId = chatId;
        msg.prompt = prompt;
        send(msg);
    }

    /** AI generálás megszakításának kérése. */
    public void sendCancel() {
        Message msg = new Message();
        msg.type = "CANCEL";
        send(msg);
    }

    private void send(Message msg) {
        out.println(gson.toJson(msg));
    }

    /**
     * @param cb callback, amelyet a CHATS_UPDATE üzenetre hívunk meg
     */
    public void setOnChatsUpdate(Consumer<List<Chat>> cb) { onChatsUpdate = cb; }

    /**
     * @param cb callback, amelyet az AI_START üzenetre hívunk meg
     */
    public void setOnAiStart(Consumer<String> cb) { onAiStart = cb; }

    /**
     * @param cb callback, amelyet az AI_CHUNK üzenetre hívunk meg
     */
    public void setOnAiChunk(Consumer<String> cb) { onAiChunk = cb; }

    /**
     * @param cb callback, amelyet az AI_DONE üzenetre hívunk meg
     */
    public void setOnAiDone(Runnable cb) { onAiDone = cb; }

    /**
     * @param cb callback, amelyet az AI_ERROR üzenetre hívunk meg
     */
    public void setOnAiError(Consumer<String> cb) { onAiError = cb; }

    /**
     * @param cb callback, amelyet a SERVER_ERROR üzenetre vagy kapcsolatvesztésre hívunk meg
     */
    public void setOnServerError(Runnable cb) { onServerError = cb; }
}
