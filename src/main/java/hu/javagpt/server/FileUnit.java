package hu.javagpt.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import hu.javagpt.model.Chat;
import hu.javagpt.model.User;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * A felhasználók és csevegések JSON fájlba mentéséért és betöltéséért felelős.
 *
 * A szerver oldalon a ServerState példányosítja,
 * és minden fájlművelet azon keresztül, szinkronizáltan történik.
 *
 * A fájlok a data/ almappában jönnek létre: data/users.json, data/chats.json.
 */
public class FileUnit {

    private static final Path DATA_DIR  = Paths.get("data");
    private static final Path USERS_FILE = DATA_DIR.resolve("users.json");
    private static final Path CHATS_FILE = DATA_DIR.resolve("chats.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static void ensureDataDir() throws IOException {
        Files.createDirectories(DATA_DIR);
    }

    /**
     * Betölti a felhasználók listáját a fájlból.
     *
     * @return a felhasználók listája, vagy üres lista ha a fájl nem létezik
     * @throws IOException ha a fájl olvasása sikertelen
     */
    public List<User> loadUsers() throws IOException {
        if (!Files.exists(USERS_FILE)) return new ArrayList<>();
        try (Reader reader = new FileReader(USERS_FILE.toFile())) {
            Type type = new TypeToken<List<User>>() {}.getType();
            List<User> users = GSON.fromJson(reader, type);
            return users != null ? users : new ArrayList<>();
        }
    }

    /**
     * Elmenti a felhasználók listáját fájlba.
     *
     * @param users a mentendő felhasználók
     * @throws IOException ha a fájl írása sikertelen
     */
    public void saveUsers(List<User> users) throws IOException {
        ensureDataDir();
        try (Writer writer = new FileWriter(USERS_FILE.toFile())) {
            GSON.toJson(users, writer);
        }
    }

    /**
     * Betölti az összes csevegést a fájlból.
     *
     * @return a csevegések listája, vagy üres lista ha a fájl nem létezik
     * @throws IOException ha a fájl olvasása sikertelen
     */
    public List<Chat> loadChats() throws IOException {
        if (!Files.exists(CHATS_FILE)) return new ArrayList<>();
        try (Reader reader = new FileReader(CHATS_FILE.toFile())) {
            Type type = new TypeToken<List<Chat>>() {}.getType();
            List<Chat> chats = GSON.fromJson(reader, type);
            return chats != null ? chats : new ArrayList<>();
        }
    }

    /**
     * Elmenti az összes csevegést fájlba.
     *
     * @param chats a mentendő csevegések
     * @throws IOException ha a fájl írása sikertelen
     */
    public void saveChats(List<Chat> chats) throws IOException {
        ensureDataDir();
        try (Writer writer = new FileWriter(CHATS_FILE.toFile())) {
            GSON.toJson(chats, writer);
        }
    }
}
