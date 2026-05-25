package hu.javagpt.client.controller;

import hu.javagpt.MainApp;
import hu.javagpt.client.ServerConnection;
import hu.javagpt.client.UserState;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * A regisztrációs képernyő vezérlője.
 *
 * Felhasználónév és jelszó megadása után regisztrációs kérelmet küld
 * a szervernek. Ha a felhasználónév foglalt,
 * a hibaszöveget jeleníti meg. A "Vissza" gomb visszanavigál a főmenüre.
 */
public class RegisterController {

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
    public RegisterController(UserState userState, ServerConnection serverConnection, MainApp app) {
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

    /** Regisztrációs kérelmet küld a szervernek a megadott adatokkal. */
    @FXML
    public void onRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            resultLabel.setStyle("-fx-text-fill: red;");
            resultLabel.setText("Töltsd ki mindkét mezőt!");
            return;
        }
        resultLabel.setText("Kérés elküldve...");
        resultLabel.setStyle("-fx-text-fill: gray;");
        serverConnection.sendRegister(username, password, result -> {
            if ("OK".equals(result)) {
                resultLabel.setStyle("");
                resultLabel.setText("Sikeres regisztráció!");
                usernameField.clear();
                passwordField.clear();
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
