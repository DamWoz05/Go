package pt.training.go.server.command.parser;

import pt.training.go.server.command.*;

/**
 * CommandParser analizuje wejściowe polecenia tekstowe i konwertuje je
 * na odpowiednie obiekty Command.
 * Obsługuje różne typy poleceń gry w Go takie jak ruchy, przejścia i rezygnacje.
 */
public class CommandParser {

    /**
     * Analizuje linię tekstową i zwraca odpowiedni obiekt Command.
     * Obsługuje polecenia: MOVE, PASS, REQUEST_RESUME, AGREE_END, TOGGLE_DEAD, RESIGN, QUIT.
     *
     * @param line wejściowe polecenie tekstowe do analizy
     * @return obiekt Command odpowiadający analizowanemu poleceniu,
     *         lub InvalidCommand/UnknownCommand jeśli analiza się nie powiedzie
     */
    public Command parse(String line) {
        if (line == null) {
            return new InvalidCommand("null");
        }
       
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return new InvalidCommand("(empty)");
        }

        String[] parts = trimmed.split("\\s+");
        String keyword = parts[0].toUpperCase();

        if (keyword.equals("MOVE")) {
            return parseMove(parts);
        } else if (keyword.equals("PASS")) {
            return new PassCommand();
        } else if (keyword.equals("REQUEST_RESUME")) {
            return new RequestResumeCommand();
        } else if (keyword.equals("AGREE_END")) {
            return new AgreeEndCommand();
        } else if (keyword.equals("TOGGLE_DEAD")) {
            return parseToggleDead(parts);
        } else if (keyword.equals("RESIGN")) {
            return new ResignCommand();
        } else if (keyword.equals("QUIT")) {
            return new QuitCommand();
        } else {
            return new UnknownCommand(trimmed);
        }
    }

    /**
     * Analizuje polecenie ruchu na planszy.
     * Oczekuje formatu: MOVE wiersz kolumna
     *
     * @param parts tablica słów polecenia podzielona wg białych znaków
     * @return MoveCommand z parsowanymi współrzędnymi lub InvalidCommand w przypadku błędu
     */
    private Command parseMove(String[] parts) {
        if (parts.length < 3) {
            return new InvalidCommand("Nieprawidlowy format komendy. Uzycie: 3 4 (wiersz kolumna)");
        }
        try {
            int row = Integer.parseInt(parts[1]) - 1;
            int col = Integer.parseInt(parts[2]) - 1;
            return new MoveCommand(row, col);
        } catch (NumberFormatException e) {
            return new InvalidCommand("Wiersz i kolumna musza byc liczbami calkowitymi.");
        }
    }

    /**
     * Analizuje polecenie oznaczenia kamienia jako martwego.
     * Oczekuje formatu: TOGGLE_DEAD wiersz kolumna
     *
     * @param parts tablica słów polecenia podzielona wg białych znaków
     * @return ToggleDeadCommand z parsowanymi współrzędnymi lub InvalidCommand w przypadku błędu
     */
    private Command parseToggleDead(String[] parts) {
        if (parts.length < 3) {
            return new InvalidCommand("Nieprawidlowy format komendy dead. Uzycie: dead 3 4");
        }
        try {
            int row = Integer.parseInt(parts[1]) - 1;
            int col = Integer.parseInt(parts[2]) - 1;
            return new ToggleDeadCommand(row, col);
        } catch (NumberFormatException e) {
            return new InvalidCommand("Wiersz i kolumna musza byc liczbami calkowitymi.");
        }
    }
}