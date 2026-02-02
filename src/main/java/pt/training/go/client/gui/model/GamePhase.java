package pt.training.go.client.gui.model;

/**
 * Faza gry po stronie klienta GUI.
 *
 * W zaleznosci od fazy zmienia sie zachowanie GUI i dozwolone akcje.
 */
public enum GamePhase {

    /** Normalna gra: ruchy, PASS i RESIGN. */
    PLAYING,

    /** Faza ustalania wyniku: reczne oznaczanie martwych grup (TOGGLE_DEAD) i akceptacja wyniku. */
    SCORING,

    REPLAY,

    /** Koniec gry: wyswietlenie wyniku i blokada dalszych akcji. */
    FINISHED
}
