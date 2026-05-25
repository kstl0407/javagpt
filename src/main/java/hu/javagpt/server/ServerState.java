package hu.javagpt.server;

import com.google.gson.Gson;
import hu.javagpt.common.Message;
import hu.javagpt.model.Chat;
import hu.javagpt.model.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A szerver megosztott állapota: aktív kliensek listája és fájlműveletek.
 * Minden metódus synchronized, így szálbiztos a több ClientHandler-szál között.
 */
public class ServerState {

    private final FileUnit fileUnit = new FileUnit();
    private final List<ClientHandler> activeClients = new ArrayList<>();
    private final Gson gson = new Gson();

    /**
     * Új kliens hozzáadása az aktív kliensek listájához.
     *
     * @param handler az új kliens kezelője
     */
    public synchronized void addClient(ClientHandler handler) {
        activeClients.add(handler);
    }

    /**
     * Kliens eltávolítása (kapcsolat bontásakor).
     *
     * @param handler az eltávolítandó kliens kezelője
     */
    public synchronized void removeClient(ClientHandler handler) {
        activeClients.remove(handler);
    }

    /**
     * JSON üzenet küldése az adott felhasználó összes aktív kapcsolatára.
     *
     * @param username a célfelhasználó neve
     * @param json     a küldendő JSON sor
     */
    public synchronized void broadcastToUser(String username, String json) {
        for (ClientHandler client : activeClients) {
            if (username.equals(client.getLoggedInUser())) {
                client.send(json);
            }
        }
    }

    /**
     * Elküldi a felhasználó aktuális csevegéslistáját az összes aktív kapcsolatára.
     *
     * @param username a felhasználó neve
     * @throws IOException ha a fájlolvasás sikertelen
     */
    public synchronized void broadcastChatsUpdateForUser(String username) throws IOException {
        List<Chat> userChats = getUserChats(username);
        Message msg = new Message();
        msg.type = "CHATS_UPDATE";
        msg.chats = userChats;
        broadcastToUser(username, gson.toJson(msg));
    }

    /**
     * @return az összes felhasználó listája
     * @throws IOException ha a fájlolvasás sikertelen
     */
    public synchronized List<User> loadUsers() throws IOException {
        return fileUnit.loadUsers();
    }

    /**
     * @param users mentendő felhasználók
     * @throws IOException ha a fájlírás sikertelen
     */
    public synchronized void saveUsers(List<User> users) throws IOException {
        fileUnit.saveUsers(users);
    }

    /**
     * @return az összes csevegés listája
     * @throws IOException ha a fájlolvasás sikertelen
     */
    public synchronized List<Chat> loadChats() throws IOException {
        return fileUnit.loadChats();
    }

    /**
     * @param chats mentendő csevegések
     * @throws IOException ha a fájlírás sikertelen
     */
    public synchronized void saveChats(List<Chat> chats) throws IOException {
        fileUnit.saveChats(chats);
    }

    /**
     * @param username a felhasználó neve
     * @return csak az adott felhasználóhoz tartozó csevegések
     * @throws IOException ha a fájlolvasás sikertelen
     */
    public synchronized List<Chat> getUserChats(String username) throws IOException {
        List<Chat> all = fileUnit.loadChats();
        List<Chat> result = new ArrayList<>();
        for (Chat c : all) {
            if (username.equals(c.getUsername())) {
                result.add(c);
            }
        }
        return result;
    }
}
