package hu.javagpt.common;

import hu.javagpt.model.Chat;

import java.util.List;

/**
 * Szerver és kliens között küldött üzenet POJO.
 * Minden üzenet egy JSON sor a socketen keresztül.
 *
 * Kliens - Szerver típusok: REGISTER, LOGIN, LOGOUT, NEW_CHAT,
 * DELETE_CHAT, SEND_PROMPT, CANCEL
 *
 * Szerver - Kliens típusok: REGISTER_OK, REGISTER_ERROR, LOGIN_OK,
 * LOGIN_ERROR, LOGOUT_OK, CHATS_UPDATE, AI_START, AI_CHUNK, AI_DONE,
 * AI_ERROR, SERVER_ERROR
 */
public class Message {

    /** Az üzenet típusa. */
    public String type;

    /** Felhasználónév (REGISTER, LOGIN). */
    public String username;

    /** Jelszó (REGISTER, LOGIN). */
    public String password;

    /** Chat azonosítója (DELETE_CHAT, SEND_PROMPT, AI_START, AI_CHUNK, AI_DONE, AI_ERROR). */
    public String chatId;

    /** Prompt szövege (SEND_PROMPT). */
    public String prompt;

    /** Hibaüzenet szövege (REGISTER_ERROR, LOGIN_ERROR). */
    public String message;

    /** AI válasz egy darabja (AI_CHUNK). */
    public String chunk;

    /** Felhasználó csevegéseinek listája (CHATS_UPDATE). */
    public List<Chat> chats;
}
