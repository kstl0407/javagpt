package hu.javagpt.client.controller;

import hu.javagpt.MainApp;
import javafx.fxml.FXML;

/**
 * A főmenü vezérlője.
 *
 * Az alkalmazás indulásakor megjelenő képernyő, amely a regisztrációs
 * és a bejelentkezési oldalra navigál.
 */
public class MenuController {

    private final MainApp app;

    /**
     * @param app az alkalmazás főosztálya a navigációhoz
     */
    public MenuController(MainApp app) {
        this.app = app;
    }

    /** A regisztrációs képernyőre navigál. */
    @FXML
    public void onRegister() { app.showRegister(); }

    /** A bejelentkezési képernyőre navigál. */
    @FXML
    public void onLogin() { app.showLogin(); }
}
