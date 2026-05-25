package hu.javagpt.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Egy csevegést reprezentál, amely egy adott felhasználóhoz tartozik.
 *
 * Az üzenetek listája kronológiai sorrendben tartalmazza a felhasználó
 * promptjait és az AI válaszait
 *
 * A szerver a chats.json fájlban tárolja Gson segítségével.
 * Az id mező UUID alapú, egyedi azonosítóként szolgál a hálózati
 * protokollban (törlés, prompt küldés).
 */
public class Chat {

    /** Egyedi azonosító (UUID). */
    private String id;

    /** A tulajdonos felhasználó neve. */
    private String username;

    /** A csevegés megjelenített neve. */
    private String title;

    /** Az üzenetek listája kronológiai sorrendben. */
    private List<String> messages;

    /**
     * @param username a tulajdonos felhasználó neve
     * @param title    a csevegés neve
     */
    public Chat(String username, String title) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.title = title;
        this.messages = new ArrayList<>();
    }

    /** @return az egyedi azonosító */
    public String getId() { return id; }

    /** @return a tulajdonos felhasználó neve */
    public String getUsername() { return username; }

    /** @return a csevegés neve */
    public String getTitle() { return title; }

    /** @return az üzenetek listája */
    public List<String> getMessages() { return messages; }
}
