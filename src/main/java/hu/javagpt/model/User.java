package hu.javagpt.model;

/**
 * Egy regisztrált felhasználót reprezentál.
 *
 * A szerver a users.json fájlban tárolja Gson segítségével.
 * A jelszó jelenleg plain text formában kerül mentésre.
 */
public class User {

    /** A felhasználó egyedi neve. */
    private String username;

    /** A felhasználó jelszava. */
    private String password;

    /**
     * @param username a felhasználó neve
     * @param password a felhasználó jelszava
     */
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /** @return a felhasználó neve */
    public String getUsername() { return username; }

    /** @return a felhasználó jelszava */
    public String getPassword() { return password; }
}
