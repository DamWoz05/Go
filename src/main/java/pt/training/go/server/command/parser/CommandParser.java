package pt.training.go.server.command.parser;

import pt.training.go.server.command.*;

public class CommandParser {

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

    private Command parseMove(String[] parts) {
        if (parts.length < 3) {
            return new InvalidCommand("Nieprawidlowy format komendy MOVE. Uzycie: 3 4 (wiersz kolumna)");
        }
        try {
            int row = Integer.parseInt(parts[1]) - 1;
            int col = Integer.parseInt(parts[2]) - 1;
            return new MoveCommand(row, col);
        } catch (NumberFormatException e) {
            return new InvalidCommand("Wiersz i kolumna musza byc liczbami calkowitymi.");
        }
    }

    private Command parseToggleDead(String[] parts) {
        if (parts.length < 3) {
            return new InvalidCommand("Nieprawidlowy format komendy TOGGLE_DEAD. Uzycie: TOGGLE_DEAD 3 4");
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