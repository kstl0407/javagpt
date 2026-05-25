package hu.javagpt.client.controller;

import hu.javagpt.MainApp;
import hu.javagpt.client.ServerConnection;
import hu.javagpt.client.UserState;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * A bejelentkezési képernyő vezérlője.
 *
 * Felhasználónév és jelszó megadása után bejelentkezési kérelmet küld
 * a szervernek. Sikeres bejelentkezés esetén a csevegési képernyőre
 * navigál, hiba esetén a hibaszöveget jeleníti meg.
 */
public class LoginController {

    private final UserState userState;
    private final ServerConnection serverConnection;
    private final MainApp app;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label resultLabel;

    /**
     * @param userState        a kliens oldali állapot
     * @param serverConnection a szerver kapcsolat
     * @param app              az alkalmazás főosztálya a navigációhoz
     */
    public LoginController(UserState userState, ServerConnection serverConnection, MainApp app) {
        this.userState = userState;
        this.serverConnection = serverConnection;
        this.app = app;
    }

    /** Alaphelyzetbe állítja a beviteli mezőket és a hibafeliratot. */
    public void reset() {
        usernameField.clear();
        passwordField.clear();
        resultLabel.setText(" ");
        resultLabel.setStyle("");
    }

    /** Bejelentkezési kérelmet küld a szervernek a megadott adatokkal. */
    @FXML
    public void onLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        serverConnection.sendLogin(username, password, result -> {
            if ("OK".equals(result)) {
                userState.setLoggedInUser(username);
                usernameField.clear();
                passwordField.clear();
                resultLabel.setText(" ");
                app.showChats();
            } else {
                resultLabel.setStyle("-fx-text-fill: red;");
                resultLabel.setText(result);
            }
        });
    }

    /** Visszanavigál a főmenüre. */
    @FXML
    public void onBack() { app.showMenu(); }
}
