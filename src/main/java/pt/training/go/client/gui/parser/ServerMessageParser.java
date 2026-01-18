package pt.training.go.client.gui.parser;

import pt.training.go.client.gui.events.ClientEventListener;

/**
 * Parser protokolu tekstowego serwera.
 *
 * Przyjmuje pojedyncze linie tekstu (np. "WELCOME...")
 * i zamienia je na wywolania metod na obiekcie listenera.
 */
public final class ServerMessageParser {

    private final ClientEventListener listener;

    /**
     * Tworzy parser i ustawia odbiornik zdarzen.
     *
     * @param listener obiekt, do ktorego beda przekazywane zdarzenia po sparsowaniu
     */
    public ServerMessageParser(ClientEventListener listener) {
        if (listener == null) throw new IllegalArgumentException("listener cannot be null");
        this.listener = listener;
    }

    /**
     * Parsuje pojedyncza linie otrzymana z serwera.
     * Dla rozpoznanych formatow wywola odpowiednia metode listenera.
     * Dla bledow formatu wywola onParseError.
     *
     * @param line surowa linia tekstu z serwera
     */
    public void parseLine(String line) {
        if (line == null) return;
        line = line.trim();
        if (line.isEmpty()) return;

        try {
            if (line.startsWith("WELCOME")) {
                String color = rest(line, "WELCOME");
                if (color.isEmpty()) {
                    listener.onParseError(line, "Brak nazwy koloru.");
                    return;
                }
                listener.onWelcome(color);
                return;
            }

            if (line.startsWith("BOARD_SIZE")) {
                String s = rest(line, "BOARD_SIZE");
                int size = Integer.parseInt(s);
                listener.onBoardSize(size);
                return;
            }

            if (line.startsWith("BOARD ")) {
                String flat = line.substring("BOARD ".length());
                listener.onBoard(flat);
                return;
            }

            if (line.equals("YOUR_MOVE")) {
                listener.onYourMove();
                return;
            }

            if (line.startsWith("MOVE_ACCEPTED")) {
                int[] rc = parseTwoInts(rest(line, "MOVE_ACCEPTED"), line);
                if (rc == null) return;
                listener.onMoveAccepted(rc[0], rc[1]);
                return;
            }

            if (line.startsWith("OPPONENT_MOVED")) {
                int[] rc = parseTwoInts(rest(line, "OPPONENT_MOVED"), line);
                if (rc == null) return;
                listener.onOpponentMoved(rc[0], rc[1]);
                return;
            }

            if (line.startsWith("MESSAGE")) {
                String msg = rest(line, "MESSAGE");
                listener.onMessage(msg);
                return;
            }

            if (line.equals("OTHER_PLAYER_LEFT")) {
                listener.onOtherPlayerLeft();
                return;
            }

            listener.onUnknownLine(line);

        } catch (Exception e) {
            listener.onParseError(line, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Zwraca czesc linii po slowie kluczowym.
     *
     * @param line cala linia
     * @param keyword slowo kluczowe (np. "WELCOME")
     * @return reszta linii po keyword
     */
    private static String rest(String line, String keyword) {
        String r = line.substring(keyword.length()).trim();
        return r;
    }

    /**
     * Parsuje dwie liczby calkowite z tekstu (row col).
     *
     * @param rest fragment linii po slowie kluczowym
     * @param originalLine oryginalna linia
     * @return tablica {row, col} lub null gdy brak poprawnych danych
     */
    private int[] parseTwoInts(String rest, String originalLine) {
        String[] parts = rest.trim().split("\\s+");
        if (parts.length < 2) {
            listener.onParseError(originalLine, "Brak dwoch liczb (row col).");
            return null;
        }
        int r = Integer.parseInt(parts[0]);
        int c = Integer.parseInt(parts[1]);
        return new int[]{r, c};
    }
}
