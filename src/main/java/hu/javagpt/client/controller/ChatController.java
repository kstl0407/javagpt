package hu.javagpt.client.controller;

import hu.javagpt.MainApp;
import hu.javagpt.client.ServerConnection;
import hu.javagpt.client.UserState;
import hu.javagpt.model.Chat;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;

/**
 * A fő csevegési képernyő vezérlője.
 *
 * Kezeli a sidebar-t (csevegéslista, új csevegés, törlés, kijelentkezés),
 * a chat megjelenítési területet (üzenetek, AI streaming, hibák),
 * valamint a prompt beviteli mezőt és a küldés/megszakítás gombokat.
 *
 * A szerver eseményekre (CHATS_UPDATE, AI_START, AI_CHUNK, AI_DONE, AI_ERROR)
 * reagálva frissíti a felületet. Több ablak esetén a remoteGenerating flag
 * különíti el a helyi és a másik ablakból érkező AI generálást.
 */
public class ChatController {

    private static final String THINKING_TEXT = "(A JavaGPT gondolkozik...)";
    private static final String ERROR_TEXT = "Hiba.";

    private final UserState userState;
    private final ServerConnection serverConnection;
    private final MainApp app;

    @FXML private VBox chatListContainer;
    @FXML private TextFlow chatFlow;
    @FXML private ScrollPane chatScroll;
    @FXML private TextField chatInput;
    @FXML private Button sendBtn;
    @FXML private Button cancelBtn;
    @FXML private Label chatTitleLabel;

    private boolean streamingStarted = false;
    private boolean remoteGenerating = false;
    private int remoteThinkingMsgStart = -1;

    /**
     * @param userState        a kliens oldali állapot
     * @param serverConnection a szerver kapcsolat
     * @param app              az alkalmazás főosztálya a navigációhoz
     */
    public ChatController(UserState userState, ServerConnection serverConnection, MainApp app) {
        this.userState = userState;
        this.serverConnection = serverConnection;
        this.app = app;
    }

    /** Regisztrálja a szerver event-kezelőket és beállítja az auto-scroll listenert. */
    @FXML
    public void initialize() {
        // Auto-görgetés, ha a tartalom nő
        chatFlow.heightProperty().addListener((obs, old, val) ->
                chatScroll.setVvalue(1.0));

        serverConnection.setOnChatsUpdate(this::handleChatsUpdate);
        serverConnection.setOnAiStart(chatId -> {
            if (userState.isGenerating()) return;
            remoteGenerating = true;
            remoteThinkingMsgStart = -1;
            userState.setGenerating(true);
            chatInput.setDisable(true);
            sendBtn.setDisable(true);
            if (userState.getCurrentChat() != null
                    && chatId.equals(userState.getCurrentChat().getId())) {
                reloadChatPane();
                remoteThinkingMsgStart = chatFlow.getChildren().size();
                appendText(THINKING_TEXT + "\n", Color.GRAY);
            }
        });
        serverConnection.setOnAiChunk(chunk -> {
            if (!streamingStarted) {
                removeFromPos(userState.getThinkingMsgStart());
                appendText("AI: " + chunk, Color.BLACK);
                streamingStarted = true;
            } else {
                appendText(chunk, Color.BLACK);
            }
        });
        serverConnection.setOnAiDone(this::handleAiDone);
        serverConnection.setOnAiError(this::handleAiError);
    }

    private void handleChatsUpdate(List<Chat> chats) {
        userState.setAllChats(chats);
        refreshSidebar();
        if (!userState.isGenerating() && userState.getCurrentChat() != null) {
            Chat updated = chats.stream()
                    .filter(c -> c.getId().equals(userState.getCurrentChat().getId()))
                    .findFirst().orElse(null);
            if (updated != null) {
                userState.setCurrentChat(updated);
                reloadChatPane();
            } else {
                userState.setCurrentChat(null);
                chatTitleLabel.setText("");
                chatFlow.getChildren().clear();
            }
        }
    }

    private void handleAiDone() {
        if (!userState.isGenerating()) return;
        if (remoteGenerating) {
            if (remoteThinkingMsgStart >= 0) removeFromPos(remoteThinkingMsgStart);
            if (userState.getCurrentChat() != null) {
                userState.getAllChats().stream()
                        .filter(c -> c.getId().equals(userState.getCurrentChat().getId()))
                        .findFirst()
                        .ifPresent(c -> { userState.setCurrentChat(c); reloadChatPane(); });
            }
            enableInput();
            remoteGenerating = false;
            remoteThinkingMsgStart = -1;
            userState.setGenerating(false);
            return;
        }
        if (!streamingStarted) {
            removeFromPos(userState.getThinkingMsgStart());
            appendText("AI: \n", Color.BLACK);
        } else {
            appendText("\n", Color.BLACK);
        }
        streamingStarted = false;

        userState.getAllChats().stream()
                .filter(c -> c.getId().equals(userState.getCurrentChat().getId()))
                .findFirst()
                .ifPresent(userState::setCurrentChat);

        enableInput();
        cancelBtn.setDisable(true);
        chatInput.requestFocus();
        userState.setGenerating(false);
    }

    private void handleAiError(String errorMsg) {
        if (!userState.isGenerating()) return;
        if (remoteGenerating) {
            if (remoteThinkingMsgStart >= 0) removeFromPos(remoteThinkingMsgStart);
            if (userState.getCurrentChat() != null) {
                userState.getAllChats().stream()
                        .filter(c -> c.getId().equals(userState.getCurrentChat().getId()))
                        .findFirst()
                        .ifPresent(c -> { userState.setCurrentChat(c); reloadChatPane(); });
            }
            enableInput();
            remoteGenerating = false;
            remoteThinkingMsgStart = -1;
            userState.setGenerating(false);
            return;
        }
        removeFromPos(userState.getThinkingMsgStart());
        String display = (errorMsg != null && !errorMsg.isBlank()) ? "Hiba: " + errorMsg : ERROR_TEXT;
        appendText(display + "\n", Color.RED);
        streamingStarted = false;
        chatInput.setText(userState.getPendingPrompt());

        userState.getAllChats().stream()
                .filter(c -> c.getId().equals(userState.getCurrentChat().getId()))
                .findFirst()
                .ifPresent(userState::setCurrentChat);

        enableInput();
        cancelBtn.setDisable(true);
        userState.setGenerating(false);
    }

    private void openChat(Chat chat) {
        userState.setCurrentChat(chat);
        chatTitleLabel.setText(chat.getTitle());
        reloadChatPane();
        refreshSidebar();
        chatInput.requestFocus();
    }

    private void reloadChatPane() {
        chatFlow.getChildren().clear();
        for (String msg : userState.getCurrentChat().getMessages()) {
            if (ERROR_TEXT.equals(msg)) {
                appendText(ERROR_TEXT + "\n", Color.RED);
            } else {
                appendText(msg + "\n", Color.BLACK);
            }
        }
    }

    /** Újraépíti a bal oldali csevegéslistát az aktuális userState alapján. */
    public void refreshSidebar() {
        chatListContainer.getChildren().clear();

        List<Chat> userChats = userState.getAllChats().stream()
                .filter(c -> c.getUsername().equals(userState.getLoggedInUser()))
                .toList();

        for (Chat chat : userChats) {
            HBox row = new HBox(3);
            row.setMaxWidth(Double.MAX_VALUE);

            Button nameBtn = new Button(chat.getTitle());
            nameBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(nameBtn, Priority.ALWAYS);
            nameBtn.setStyle("-fx-alignment: CENTER-LEFT; -fx-background-color: transparent; -fx-border-color: transparent;");
            if (userState.getCurrentChat() != null
                    && chat.getId().equals(userState.getCurrentChat().getId())) {
                nameBtn.setStyle(nameBtn.getStyle() + "-fx-font-weight: bold;");
            }
            nameBtn.setOnAction(e -> openChat(chat));

            Button deleteBtn = new Button("✕");
            deleteBtn.setOnAction(e -> serverConnection.sendDeleteChat(chat.getId()));

            row.getChildren().addAll(nameBtn, deleteBtn);
            chatListContainer.getChildren().add(row);
        }

        if (userChats.isEmpty()) {
            Label empty = new Label("Még nincs csevegés");
            empty.setStyle("-fx-text-fill: gray;");
            chatListContainer.getChildren().add(empty);
        }
    }

    /**
     * Kijelentkezéskor vagy szerverhiba esetén alaphelyzetbe állítja a csevegési nézetet:
     * törli a csevegéstartalmat, a sidebárt és az átmeneti állapotokat.
     */
    public void clearChat() {
        chatFlow.getChildren().clear();
        chatTitleLabel.setText("");
        chatListContainer.getChildren().clear();
        userState.setGenerating(false);
        streamingStarted = false;
        remoteGenerating = false;
        remoteThinkingMsgStart = -1;
        enableInput();
        cancelBtn.setDisable(true);
    }

    /** Új csevegés létrehozásának kérelme. */
    @FXML
    public void onNewChat() { serverConnection.sendNewChat(); }

    /** Kijelentkezés: elküldi a kérelmet, alaphelyzetbe állítja az állapotot és a főmenübe dob. */
    @FXML
    public void onLogout() {
        serverConnection.sendLogout();
        userState.setLoggedInUser(null);
        userState.setAllChats(new ArrayList<>());
        userState.setCurrentChat(null);
        clearChat();
        app.showMenu();
    }

    /** A beviteli mezőben lévő promptot elküldi a szervernek, majd letiltja a bevitelt a generálás idejére. */
    @FXML
    public void onSend() {
        String text = chatInput.getText().trim();
        if (text.isEmpty() || userState.getCurrentChat() == null || userState.isGenerating()) return;

        userState.setPendingPrompt(text);
        userState.setPendingMsgStart(chatFlow.getChildren().size());

        appendText(userState.getLoggedInUser() + ": " + text + "\n", Color.BLACK);
        chatInput.clear();

        chatInput.setDisable(true);
        sendBtn.setDisable(true);
        cancelBtn.setDisable(false);
        userState.setGenerating(true);

        userState.setThinkingMsgStart(chatFlow.getChildren().size());
        appendText(THINKING_TEXT + "\n", Color.GRAY);

        streamingStarted = false;
        serverConnection.sendPrompt(userState.getCurrentChat().getId(), text);
    }

    /** Megszakítja a folyamatban lévő AI generálást, visszaállítja a promptot a beviteli mezőbe. */
    @FXML
    public void onCancel() {
        serverConnection.sendCancel();
        userState.setGenerating(false);
        streamingStarted = false;
        removeFromPos(userState.getPendingMsgStart());
        chatInput.setText(userState.getPendingPrompt());
        enableInput();
        cancelBtn.setDisable(true);
        chatInput.requestFocus();
    }

    private void appendText(String text, Color color) {
        Text t = new Text(text);
        t.setFill(color);
        chatFlow.getChildren().add(t);
    }

    private void removeFromPos(int startPos) {
        if (startPos < 0 || startPos >= chatFlow.getChildren().size()) return;
        chatFlow.getChildren().subList(startPos, chatFlow.getChildren().size()).clear();
    }

    private void enableInput() {
        chatInput.setDisable(false);
        sendBtn.setDisable(false);
    }
}
