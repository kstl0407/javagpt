package hu.javagpt;

import hu.javagpt.client.ServerConnection;
import hu.javagpt.client.UserState;
import hu.javagpt.client.controller.ChatController;
import hu.javagpt.client.controller.LoginController;
import hu.javagpt.client.controller.MenuController;
import hu.javagpt.client.controller.RegisterController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;

/**
 * A JavaGPT kliens belépési pontja.
 *
 * Felépíti a JavaFX ablakot, kapcsolódik a szerverhez, és kezeli
 * az oldalak közötti navigációt (főmenü, regisztráció, bejelentkezés, csevegés).
 * Az összes FXML-t és controllert egyszerre tölti be az induláskor,
 * majd a scene root cseréjével vált oldalak között.
 *
 * Szerverhiba esetén kijelentkezteti a felhasználót és visszanavigál a főmenüre.
 */
public class MainApp extends Application {

    private Stage stage;
    private Scene scene;
    private ServerConnection serverConnection;
    private UserState userState;

    private Parent menuRoot, registerRoot, loginRoot, chatRoot;
    private RegisterController registerController;
    private LoginController loginController;
    private ChatController chatController;

    /**
     * Az alkalmazás indítási metódusa: kapcsolódik a szerverhez, betölti az FXML-eket,
     * beállítja a szerverhiba-kezelőt, majd megjeleníti a főmenüt.
     *
     * @param stage az elsődleges JavaFX ablak
     * @throws IOException ha valamelyik FXML betöltése sikertelen
     */
    @Override
    public void start(Stage stage) throws IOException {
        this.stage = stage;
        serverConnection = new ServerConnection();
        userState = new UserState();

        try {
            serverConnection.connect("localhost", 12345);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Nem lehet csatlakozni a szerverhez!\n" + e.getMessage());
            alert.setTitle("Kapcsolódási hiba");
            alert.setHeaderText(null);
            alert.showAndWait();
            Platform.exit();
            return;
        }

        // FXML-ek és controllerek betöltése egyszerre (initialize() itt hívódik)
        menuRoot = load("menu.fxml", new MenuController(this));

        registerController = new RegisterController(userState, serverConnection, this);
        registerRoot = load("register.fxml", registerController);

        loginController = new LoginController(userState, serverConnection, this);
        loginRoot = load("login.fxml", loginController);

        chatController = new ChatController(userState, serverConnection, this);
        chatRoot = load("chat.fxml", chatController);

        serverConnection.setOnServerError(() -> {
            userState.setLoggedInUser(null);
            userState.setAllChats(new ArrayList<>());
            userState.setCurrentChat(null);
            chatController.clearChat();
            showMenu();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Szerverhiba! Kijelentkeztetés.");
            alert.setTitle("Hiba");
            alert.setHeaderText(null);
            alert.show();
        });

        scene = new Scene(menuRoot, 300, 250);
        stage.setTitle("JavaGPT");
        stage.setScene(scene);
        stage.show();
    }

    /** A főmenő képernyőre vált és az ablakot 300×250-re állítja. */
    public void showMenu() {
        stage.setWidth(300);
        stage.setHeight(250);
        scene.setRoot(menuRoot);
        Platform.runLater(stage::centerOnScreen);
    }

    /** A regisztrációs képernyőre vált (a beviteli mezőket alaphelyzetbe állítja). */
    public void showRegister() {
        registerController.reset();
        scene.setRoot(registerRoot);
    }

    /** A bejelentkezési képernyőre vált (a beviteli mezőket alaphelyzetbe állítja). */
    public void showLogin() {
        loginController.reset();
        scene.setRoot(loginRoot);
    }

    /** A csevegési képernyőre vált és az ablakot 620×480-ra állítja. */
    public void showChats() {
        stage.setWidth(620);
        stage.setHeight(480);
        scene.setRoot(chatRoot);
        Platform.runLater(() -> {
            stage.centerOnScreen();
            chatController.refreshSidebar();
        });
    }

    /**
     * FXML betöltése a megadott controller példányhoz.
     * A setControllerFactory biztosítja, hogy a paraméterezett konstruktorú
     * controller is működjön az FXML-ben megadott fx:controller attribútummal.
     *
     * @param fxml       az FXML fájl neve (pl. "chat.fxml")
     * @param controller a controller példány
     * @return a betöltött JavaFX gyökérelem
     * @throws IOException ha a fájl nem található vagy betöltési hiba van
     */
    private Parent load(String fxml, Object controller) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
        loader.setControllerFactory(clazz -> controller);
        return loader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}
