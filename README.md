# JavaGPT

Kliens-szerver alapú Java chat alkalmazás JavaFX grafikus felülettel, amely egy lokálisan futó Ollama AI modellel kommunikál. A szerver egyszerre több klienst is kiszolgál, és valós idejű streamelt AI válaszokat küld TCP socketen keresztül.

---

## Technológiák

| Réteg | Technológia |
|---|---|
| Nyelv | Java 25 |
| GUI | JavaFX 21 + FXML |
| Build | Apache Maven |
| AI integráció | [ollama4j](https://github.com/ollama4j/ollama4j) 1.0.79 |
| AI modell | Ollama – gemma4 |
| Serializáció | Gson 2.10.1 |
| Adattárolás | JSON fájlok (users.json, chats.json) |
| Kommunikáció | TCP socket, soronkénti JSON protokoll |



---

## Előfeltételek

- **Java 25+**
- **Apache Maven**
- **Ollama** telepítve és futó állapotban
  - Letöltés: https://ollama.com
  - Szükséges modell: `gemma4`


---

## Projekt indítása

A szerver és a kliens két külön folyamatként fut. **Először a szervert kell elindítani.**

### 1. Szerver indítása

Futtasd a `ServerMain` osztályt:

```
hu.javagpt.server.ServerMain
```

Sikeres indítás esetén a konzolon megjelenik:

```
JavaGPT szerver elindult, port: 12345
```

### 2. Kliens indítása

Futtasd a `MainApp` osztályt:

```
hu.javagpt.MainApp
```

Több kliens ablak is megnyitható egyszerre – mindegyik saját bejelentkezési munkamenettel rendelkezik.

---

## Használat

1. Indítsd el a szervert, majd a klienst
2. A főmenüben válaszd a **Regisztráció** lehetőséget, és hozz létre egy fiókot
3. Jelentkezz be a felhasználóneveddel és jelszavaddal
4. Hozz létre új csevegést a **＋ Új csevegés** gombbal
5. Írd be a promptot, és küldd el a **Küldés** gombbal vagy az **Enter** billentyűvel
6. A generálás közben a **Megszakítás** gombbal leállítható az AI válasz

---

## Projekt struktúra


```
src/main/java/hu/javagpt/
├── MainApp.java                    # JavaFX belépési pont
├── common/
│   └── Message.java                # Kliens-szerver kommunikációs POJO
├── model/
│   ├── User.java                   # Felhasználó modell
│   └── Chat.java                   # Csevegés modell 
├── server/
│   ├── ServerMain.java             # Szerver belépési pont
│   ├── ServerState.java            # Megosztott szerver állapot 
│   ├── ClientHandler.java          # Egy kliens kapcsolat kezelése 
│   ├── OllamaService.java          # Ollama AI integráció streamelt generálással
│   └── FileUnit.java               # JSON fájl olvasás/írás
└── client/
    ├── ServerConnection.java       # TCP kapcsolat a szerverrel
    ├── UserState.java              # Bejelentkezett felhasználó állapota
    └── controller/
        ├── LoginController.java
        ├── RegisterController.java
        ├── MenuController.java
        └── ChatController.java

data/
├── users.json                      # Felhasználók adatai
└── chats.json                      # Csevegések és üzenetek
```

---

## Kommunikációs protokoll

Az üzenetek JSON sorok TCP socketen keresztül. A `Message` POJO tartalmazza az összes mezőt.

| Irány | Típus | Leírás |
|---|---|---|
| K → S | `REGISTER` | Regisztráció |
| K → S | `LOGIN` | Bejelentkezés |
| K → S | `LOGOUT` | Kijelentkezés |
| K → S | `NEW_CHAT` | Új csevegés létrehozása |
| K → S | `DELETE_CHAT` | Csevegés törlése |
| K → S | `SEND_PROMPT` | Prompt küldése |
| K → S | `CANCEL` | Folyamatban lévő generálás megszakítása |
| S → K | `LOGIN_OK` / `LOGIN_ERROR` | Bejelentkezés eredménye |
| S → K | `CHATS_UPDATE` | Csevegések listájának frissítése |
| S → K | `AI_START` | Generálás kezdete |
| S → K | `AI_CHUNK` | AI válasz egy darabja|
| S → K | `AI_DONE` | Generálás vége |
| S → K | `AI_ERROR` | Generálási hiba |
