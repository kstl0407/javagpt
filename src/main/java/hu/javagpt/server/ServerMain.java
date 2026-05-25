package hu.javagpt.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A JavaGPT szerver belépési pontja.
 *
 * Elindít egy ServerSocket-et a megadott porton, majd minden
 * beérkező kliens kapcsolathoz létrehoz egy ClientHandler-t egy új szálban.
 * A főszál folyamatosan várja az újabb kapcsolatokat (accept() ciklus).
 */
public class ServerMain {

    /** A szerver portszáma. */
    private static final int PORT = 12345;

    /**
     * @param args parancssori argumentumok (nem használtak)
     */
    public static void main(String[] args) {
        ServerState state = new ServerState();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("JavaGPT szerver elindult, port: " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                try {
                    ClientHandler handler = new ClientHandler(clientSocket, state);
                    new Thread(handler).start();
                } catch (IOException e) {
                    System.err.println("Kliens kapcsolat hiba: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Szerver indítási hiba: " + e.getMessage());
        }
    }
}
