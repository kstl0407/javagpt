package hu.javagpt.client;

import hu.javagpt.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A kliens oldali alkalmazás állapotát tárolja.
 *
 * Tartalmazza a bejelentkezett felhasználó adatait, a csevegések listáját,
 * valamint az AI generáláshoz szükséges átmeneti UI állapotot
 * (pl. a szövegpanel pozíciói a gondolkozás jelző eltávolításához).
 */
public class UserState {

    /** A bejelentkezett felhasználó neve, vagy null ha nincs bejelentkezve. */
    private String loggedInUser;

    /** A bejelentkezett felhasználó csevegéseinek listája (szerverről szinkronizálva). */
    private List<Chat> allChats = new ArrayList<>();

    /** Az éppen megnyitott csevegés, vagy null ha nincs megnyitva. */
    private Chat currentChat;

    /** Igaz, amíg az AI éppen generál választ. */
    private boolean isGenerating = false;

    /** Az elküldött prompt szövege (megszakítás esetén visszakerül a beviteli mezőbe). */
    private String pendingPrompt = "";

    /** A felhasználó üzenetének kezdőpozíciója a szövegpanelben. */
    private int pendingMsgStart = 0;

    /** A gondolkozás jelző szöveg kezdőpozíciója a szövegpanelben. */
    private int thinkingMsgStart = 0;

    /** @return a bejelentkezett felhasználó neve, vagy null */
    public String getLoggedInUser() { return loggedInUser; }

    /** @param loggedInUser a bejelentkezett felhasználó neve */
    public void setLoggedInUser(String loggedInUser) { this.loggedInUser = loggedInUser; }

    /** @return a felhasználó csevegéseinek listája */
    public List<Chat> getAllChats() { return allChats; }

    /** @param allChats az új csevegéslista */
    public void setAllChats(List<Chat> allChats) { this.allChats = allChats; }

    /** @return az éppen megnyitott csevegés */
    public Chat getCurrentChat() { return currentChat; }

    /** @param currentChat az éppen megnyitott csevegés */
    public void setCurrentChat(Chat currentChat) { this.currentChat = currentChat; }

    /** @return igaz, ha az AI éppen generál */
    public boolean isGenerating() { return isGenerating; }

    /** @param generating igaz, ha az AI éppen generál */
    public void setGenerating(boolean generating) { isGenerating = generating; }

    /** @return az elküldött prompt szövege */
    public String getPendingPrompt() { return pendingPrompt; }

    /** @param pendingPrompt az elküldött prompt szövege */
    public void setPendingPrompt(String pendingPrompt) { this.pendingPrompt = pendingPrompt; }

    /** @return a felhasználó üzenetének kezdőpozíciója a szövegpanelben */
    public int getPendingMsgStart() { return pendingMsgStart; }

    /** @param pendingMsgStart a felhasználó üzenetének kezdőpozíciója */
    public void setPendingMsgStart(int pendingMsgStart) { this.pendingMsgStart = pendingMsgStart; }

    /** @return a gondolkozás jelző szöveg kezdőpozíciója a szövegpanelben */
    public int getThinkingMsgStart() { return thinkingMsgStart; }

    /** @param thinkingMsgStart a gondolkozás jelző szöveg kezdőpozíciója */
    public void setThinkingMsgStart(int thinkingMsgStart) { this.thinkingMsgStart = thinkingMsgStart; }
}
