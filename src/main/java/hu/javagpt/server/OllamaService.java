package hu.javagpt.server;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.generate.OllamaStreamHandler;
import io.github.ollama4j.utils.OptionsBuilder;

import java.util.function.Consumer;

/**
 * Az Ollama modellel kommunikál az ollama4j könyvtár segítségével.
 *
 * A generate metódus egy külön szálban fut, és a callback-eket
 * közvetlenül hívja meg, mert a szerveren fut.
 */
public class OllamaService {

    private static final String MODEL = "gemma4";
    private static final String OLLAMA_HOST = "http://localhost:11434";

    private final OllamaAPI ollamaAPI;
    private volatile boolean cancelled = false;
    private volatile Thread generationThread;

    /**
     * Létrehozza az OllamaAPI klienst az alapértelmezett localhost:11434 hoston.
     */
    public OllamaService() {
        ollamaAPI = new OllamaAPI(OLLAMA_HOST);
        ollamaAPI.setRequestTimeoutSeconds(120);
    }

    /**
     * Megszakítja a folyamatban lévő generálást.
     */
    public void cancel() {
        cancelled = true;
        if (generationThread != null) {
            generationThread.interrupt();
        }
    }

    /**
     * AI választ generál az ollama4j segítségével.
     *
     * @param prompt   a felhasználó üzenete
     * @param onChunk  minden beérkező chunk esetén hívódik meg
     * @param onDone   a generálás sikeres befejezésekor hívódik meg
     * @param onError  hiba esetén hívódik meg
     */
    public void generate(String prompt,
                         Consumer<String> onChunk,
                         Runnable onDone,
                         Consumer<Exception> onError) {
        cancelled = false;
        generationThread = new Thread(() -> {
            try {
                StringBuilder previous = new StringBuilder();
                OllamaStreamHandler streamHandler = chunk -> {
                    if (!cancelled && !Thread.currentThread().isInterrupted()) {
                        String delta = chunk.substring(previous.length());
                        previous.setLength(0);
                        previous.append(chunk);
                        if (!delta.isEmpty()) {
                            onChunk.accept(delta);
                        }
                    }
                };

                ollamaAPI.generate(MODEL, prompt, false, new OptionsBuilder().build(), streamHandler);

                if (!cancelled) {
                    onDone.run();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                if (cancelled) return;
                System.err.println("[OllamaService] Hiba: " + e.getMessage());
                e.printStackTrace();
                onError.accept(e);
            }
        }, "ollama-thread");
        generationThread.start();
    }
}
