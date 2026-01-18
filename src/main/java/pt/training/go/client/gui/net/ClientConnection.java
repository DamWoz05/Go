package pt.training.go.client.gui.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Polaczenie klienta z serwerem.
 *
 * Klasa:
 * - nawiazuje polaczenie socketem,
 * - wysyla komendy jako pojedyncze linie tekstu,
 * - odbiera linie tekstu w osobnym watku i przekazuje je do listenera.
 */
public final class ClientConnection implements AutoCloseable {

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final Thread readerThread;

    private volatile boolean running = true;

    /**
     * Laczy sie z serwerem i uruchamia watek czytajacy linie z polaczenia.
     *
     * @param host host serwera (np. "localhost")
     * @param port port serwera
     * @param listener odbiornik linii z serwera
     * @throws IOException gdy nie uda sie nawiazac polaczenia
     */
    public ClientConnection(String host, int port, ServerLineListener listener) throws IOException {
        if (listener == null) throw new IllegalArgumentException("listener cannot be null");

        this.socket = new Socket(host, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        this.readerThread = new Thread(() -> readLoop(listener), "go-client-reader");
        this.readerThread.setDaemon(true);
        this.readerThread.start();
    }

    /**
     * Petla odbioru danych z serwera.
     * W razie rozlaczenia lub bledu informuje listener.
     *
     * @param listener odbiornik linii/rozlaczenia
     */
    private void readLoop(ServerLineListener listener) {
        try {
            while (running) {
                String line = in.readLine();
                if (line == null) {
                    listener.onDisconnected("Polaczenie zamkniete przez serwer.");
                    return;
                }
                listener.onLine(line);
            }
        } catch (IOException e) {
            if (running) {
                listener.onDisconnected("Blad polaczenia: " + e.getMessage());
            }
        } catch (Exception e) {
            if (running) {
                listener.onDisconnected("Nieoczekiwany blad: " + e.getMessage());
            }
        }
    }

    /**
     * Wysyla pojedyncza linie do serwera.
     *
     * @param line linia do wyslania (bez znaku nowej linii)
     */
    public void sendLine(String line) {
        if (!running) return;
        out.println(line);
    }

    /**
     * @return true jesli polaczenie jest w trybie running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Zamyka polaczenie i konczy watek odbioru.
     */
    @Override
    public void close() {
        running = false;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
