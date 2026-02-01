package pt.training.go.server.command;

import pt.training.go.server.Board;
import pt.training.go.server.state.GameState;

/**
 * GameContext definiuje interfejs do zarządzania stanem gry w Go.
 * Zapewnia metody do kontroli przebiegu gry, zarządzania planszą,
 * obsługi ruchów graczy i komunikacji między graczami.
 */
public interface GameContext {
    /**
     * Zwraca obiekt synchronizacyjny do bezpiecznego dostępu do stanu gry.
     *
     * @return obiekt lock do synchronizacji
     */
    Object lock();
   
    /**
     * Ustawia nowy stan gry.
     *
     * @param state nowy stan gry do ustawienia
     */
    void setState(GameState state);
   
    /**
     * Ustawia gracza, którego jest teraz kolej.
     *
     * @param player gracz, którego kolej się zaczyna
     */
    void setCurrentPlayer(PlayerContext player);
   
    /**
     * Zwraca planszę gry.
     *
     * @return obiekt Board reprezentujący planszę
     */
    Board getBoard();
    
    /**
     * Sprawdza, czy jest kolej podanego gracza.
     *
     * @param player gracz do sprawdzenia
     * @return true jeśli jest kolej tego gracza, false w innym wypadku
     */
    boolean isCurrentPlayer(PlayerContext player);
    
    /**
     * Zmienia kolej na drugiego gracza.
     */
    void switchTurn();
    
    /**
     * Wysyła wiadomość do obu graczy.
     *
     * @param message wiadomość do wysłania
     */
    void broadcast(String message);

    /**
     * Zwraca liczbę kolejnych przejść.
     *
     * @return liczba kolejnych przejść
     */
    int getConsecutivePasses();
    
    /**
     * Resetuje licznik kolejnych przejść do zera.
     */
    void resetPasses();
    
    /**
     * Zwiększa licznik kolejnych przejść o jeden.
     */
    void incrementPasses();

    /**
     * Zwraca tablicę oznaczającą martwe kamienie na planszy.
     *
     * @return tablica boolean[][] reprezentująca martwe kamienie
     */
    boolean[][] getDeadMarks();
    
    /**
     * Czyści wszystkie oznaczenia martwych kamieni.
     */
    void clearDeadMarks();
    
    /**
     * Przełącza oznaczenie martwego kamienia na podanej pozycji.
     *
     * @param row wiersz pozycji
     * @param col kolumna pozycji
     * @param player gracz wykonujący operację
     */
    void toggleDead(int row, int col, PlayerContext player);
    
    /**
     * Przełącza oznaczenie martwego kamienia za pomocą komendy.
     *
     * @param row wiersz pozycji
     * @param col kolumna pozycji
     * @param player gracz wykonujący operację
     */
    void toggleDeadCommand(int row, int col, PlayerContext player);
    
    /**
     * Zastosowuje oznaczenia martwych kamieni do planszy.
     */
    void applyDeadMarks();
   
    /**
     * Wykonuje ruch gracza na podanej pozycji.
     *
     * @param row wiersz pozycji ruchu
     * @param col kolumna pozycji ruchu
     * @param player gracz wykonujący ruch
     */
    void makeMove(int row, int col, PlayerContext player);
    
    /**
     * Wykonuje przejście dla gracza.
     *
     * @param player gracz przechodzący
     */
    void pass(PlayerContext player);
    
    /**
     * Obsługuje żądanie wznowienia gry.
     *
     * @param player gracz żądający wznowienia
     */
    void requestResume(PlayerContext player);
    
    /**
     * Obsługuje zgodę na zakończenie gry.
     *
     * @param player gracz wyrażający zgodę
     */
    void agreeEnd(PlayerContext player);
    
    /**
     * Obsługuje rezygnację gracza.
     *
     * @param player gracz rezygnujący
     */
    void resign(PlayerContext player);
    
    /**
     * Obsługuje odłączenie gracza.
     *
     * @param player gracz opuszczający grę
     */
    void quit(PlayerContext player);
   
    /**
     * Zwraca spłaszczoną reprezentację planszy jako String.
     *
     * @return String reprezentujący stan planszy
     */
    String boardFlat();
    
    /**
     * Zwraca rozmiar planszy.
     *
     * @return rozmiar planszy (liczba wierszy/kolumn)
     */
    int getBoardSize();

    /**
     * Zapisuje zwycięzcę gry do bazy danych.
     *
     * @param winner zwycięzca (CZARNY/BIALY/REMIS)
     */
    void saveWinner(String winner);
}