package pt.training.go.client.gui.net;

public interface ServerLineListener {
    /** Wywolywane dla kazdej linii otrzymanej z serwera. */
    void onLine(String line);

    /** Wywolywane gdy polaczenie zostalo zerwane lub zamkniete. */
    void onDisconnected(String reason);
}
