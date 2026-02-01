package pt.training.go.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import pt.training.go.server.command.Command;
import pt.training.go.server.command.GameContext;
import pt.training.go.server.command.PlayerContext;
import pt.training.go.server.command.parser.CommandParser;
import pt.training.go.server.db.GameEntity;
import pt.training.go.server.db.GameRepository;
import pt.training.go.server.db.MoveEntity;
import pt.training.go.server.db.MoveRepository;
import pt.training.go.server.state.FinishedState;
import pt.training.go.server.state.GameState;
import pt.training.go.server.state.PlayingState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * GoServer uruchamia serwer gry Go nasłuchujący na porcie 1988.
 * Tworzy nowe instancje gry dla par graczy i zarządza połączeniami.
 * Automatycznie zapisuje gry do bazy danych.
 */
@SpringBootApplication
public class GoServer {

    private static final int PORT = 1988;
    private static final int REPLAY_PORT = 1989;

    /**
     * Główny punkt wejścia serwera. Tworzy gniazdo nasłuchujące i
     * akceptuje pary graczy, uruchamiając nowe gry.
     *
     * @param args argumenty wiersza poleceń (nieużywane)
     * @throws IOException w przypadku błędów IO związanych z gniazdem
     */
    public static void main(String[] args) throws IOException {
        // Uruchomienie Spring Boot - potrzebne dla JPA
        ConfigurableApplicationContext context = SpringApplication.run(GoServer.class, args);
        
        // Pobieramy beany z Spring context
        GameRepository gameRepo = context.getBean(GameRepository.class);
        MoveRepository moveRepo = context.getBean(MoveRepository.class);

        int size = 19;

        // Wątek serwera REPLAY (port 1989)
        new Thread(() -> {
            try (ServerSocket replayListener = new ServerSocket(REPLAY_PORT)) {
                System.out.println("Replay server listening on port " + REPLAY_PORT);
                while (true) {
                    Socket socket = replayListener.accept();
                    new Thread(new ReplaySession(socket, moveRepo, size)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        System.out.println("Go server starting on port " + PORT + ", board size = " + size);
        System.out.println("Database enabled - games will be saved automatically");

        try (ServerSocket listener = new ServerSocket(PORT)) {
            while (true) {
                Game game = new Game(size, gameRepo, moveRepo);
                System.out.println("Oczekiwanie na graczy..");

                Game.Player black = game.new Player(listener.accept(), StoneColor.CZARNY);
                System.out.println("Gracz Czarny dolaczyl.");

                Game.Player white = game.new Player(listener.accept(), StoneColor.BIALY);
                System.out.println("Gracz Bialy dolaczyl.");

                black.setOpponent(white);
                white.setOpponent(black);

                game.setPlayers(black, white);
                game.setCurrentPlayer(black);

                new Thread(black).start();
                new Thread(white).start();

                System.out.println("Rozpoczeto gre. ID w bazie: " + game.getDbGameId());
            }
        }
    }

    /**
     * Klasa obsługująca sesję odtwarzania gry (Replay).
     */
    private static class ReplaySession implements Runnable {
        private final Socket socket;
        private final MoveRepository moveRepo;
        private final int size;

        ReplaySession(Socket socket, MoveRepository moveRepo, int size) {
            this.socket = socket;
            this.moveRepo = moveRepo;
            this.size = size;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                // Oczekujemy komendy LOAD <id>
                String loadLine = in.readLine();
                if (loadLine == null || !loadLine.startsWith("LOAD")) {
                    return;
                }
                
                Long gameId = Long.parseLong(loadLine.split(" ")[1]);
                List<MoveEntity> moves = moveRepo.findByGameIdOrderByMoveNumberAsc(gameId);
                
                // DEBUG
                System.out.println("[REPLAY] Klient zazadal gry ID: " + gameId + ". Znaleziono ruchow: " + moves.size());

                Board replayBoard = new Board(size);
                int currentMoveIndex = -1;

                out.println("WELCOME REPLAY");
                out.println("BOARD_SIZE " + size);
                out.println("BOARD " + replayBoard.toFlatString());
                out.println("MESSAGE Tryb Replay. Gra ID: " + gameId + ". Ruchow: " + moves.size());

                while (true) {
                    String line = in.readLine();
                    if (line == null || line.equalsIgnoreCase("QUIT")) break;

                    boolean changed = false;

                    if (line.equalsIgnoreCase("NEXT")) {
                        if (currentMoveIndex + 1 < moves.size()) {
                            currentMoveIndex++;
                            changed = true;
                        } else {
                            out.println("MESSAGE Koniec gry. Nie ma nowszych ruchow.");
                        }
                    } else if (line.equalsIgnoreCase("PREV")) {
                        if (currentMoveIndex >= 0) {
                            currentMoveIndex--;
                            changed = true;
                        } else {
                            out.println("MESSAGE Poczatek gry. Nie mozna cofnac.");
                        }
                    }

                    if (changed) {
                        // Reset planszy
                        for(int r=0; r<size; r++) 
                            for(int c=0; c<size; c++) 
                                replayBoard.grid[r][c] = Board.EMPTY;
                        
                        // Odtwarzanie ruchów
                        for (int i = 0; i <= currentMoveIndex; i++) {
                            MoveEntity m = moves.get(i);
                            if ("MOVE".equals(m.getType())) {
                                StoneColor c = m.getColor().equals("CZARNY") ? StoneColor.CZARNY : StoneColor.BIALY;
                                replayBoard.forcePlaceStone(m.getRowCoord(), m.getColCoord(), c);
                            }
                        }
                        
                        out.println("BOARD " + replayBoard.toFlatString());
                        
                        // Generowanie szczegółowej wiadomości
                        if (currentMoveIndex >= 0) {
                            MoveEntity m = moves.get(currentMoveIndex);
                            String info = "";
                            if ("MOVE".equals(m.getType())) {
                                info = "Ruch " + m.getColor() + " (" + (m.getRowCoord()+1) + ", " + (m.getColCoord()+1) + ")";
                            } else if ("PASS".equals(m.getType())) {
                                info = m.getColor() + " SPASOWAL";
                            } else if ("RESIGN".equals(m.getType())) {
                                info = m.getColor() + " PODDAL GRE";
                            } else {
                                info = m.getType();
                            }
                            
                            out.println("MESSAGE " + info + " [" + (currentMoveIndex + 1) + "/" + moves.size() + "]");
                        } else {
                            out.println("MESSAGE Poczatek gry (pusta plansza).");
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Replay error: " + e.getMessage());
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Game reprezentuje kontekst pojedynczej rozgrywki.
     * Implementuje interfejs GameContext i zarządza stanem gry, planszą,
     * kolejką graczy oraz komunikacją pomiędzy nimi.
     * Automatycznie zapisuje wszystkie ruchy do bazy danych.
     */
    private static class Game implements GameContext {

        private final Board board;
        private Player currentPlayer;
        private GameState state;

        private Player blackPlayer;
        private Player whitePlayer;

        private int consecutivePasses = 0;

        private boolean gameEnded = false;

        private boolean[][] deadMarks;
        
        // Baza danych
        private final GameRepository gameRepo;
        private final MoveRepository moveRepo;
        private Long dbGameId;
        private int moveCounter = 0;

        /**
         * Tworzy nową grę i automatycznie zapisuje ją do bazy danych.
         *
         * @param size rozmiar planszy
         * @param gameRepo repozytorium gier
         * @param moveRepo repozytorium ruchów
         */
        Game(int size, GameRepository gameRepo, MoveRepository moveRepo) {
            this.board = new Board(size);
            this.state = new PlayingState();
            this.deadMarks = new boolean[size][size];
            this.gameRepo = gameRepo;
            this.moveRepo = moveRepo;
            
            // Tworzymy grę w bazie danych
            GameEntity entity = new GameEntity(size);
            this.gameRepo.save(entity);
            this.dbGameId = entity.getId();
            
            System.out.println("Utworzono grę w bazie danych. ID: " + dbGameId);
        }

        /**
         * Zwraca ID gry w bazie danych.
         *
         * @return ID gry
         */
        public Long getDbGameId() {
            return dbGameId;
        }

        synchronized void setPlayers(Player black, Player white) {
            this.blackPlayer = black;
            this.whitePlayer = white;
        }

        synchronized void setCurrentPlayer(Player player) {
            this.currentPlayer = player;
        }

        /**
         * Zwraca obiekt używany do synchronizacji dostępu do stanu gry.
         *
         * @return obiekt lock do synchronizacji
         */
        @Override
        public Object lock() {
            return this;
        }

        /**
         * Ustawia nowy stan gry i wykonuje dodatkowe operacje powiązane z jego zmianą.
         *
         * @param state nowy stan gry
         */
        @Override
        public synchronized void setState(GameState state) {
            this.state = state;
            System.out.println("Stan gry zmieniony na: " + state.getName());

            if (state instanceof FinishedState) {
                gameEnded = true;
            }
        }

        /**
         * Ustawia obecnego gracza (którego jest kolej).
         *
         * @param player gracz, którego kolej ma zostać ustawiona
         */
        @Override
        public synchronized void setCurrentPlayer(PlayerContext player) {
            this.currentPlayer = (Player) player;
        }

        /**
         * Zwraca referencję do planszy gry.
         *
         * @return obiekt Board reprezentujący planszę
         */
        @Override
        public Board getBoard() {
            return board;
        }

        /**
         * Sprawdza, czy podany gracz jest aktualnym graczem (ma kolej).
         *
         * @param player kontekst gracza do sprawdzenia
         * @return true jeśli jest jego kolej, false w przeciwnym razie
         */
        @Override
        public synchronized boolean isCurrentPlayer(PlayerContext player) {
            return player == currentPlayer;
        }

        /**
         * Zmienia turę na przeciwnika i powiadamia go o ruchu.
         */
        @Override
        public synchronized void switchTurn() {
            if (currentPlayer != null) {
                currentPlayer = currentPlayer.opponent;
                if (currentPlayer != null) {
                    currentPlayer.send("YOUR_MOVE");
                }
            }
        }

        /**
         * Wysyła wiadomość do obu graczy w grze.
         *
         * @param message treść wiadomości
         */
        @Override
        public synchronized void broadcast(String message) {
            if (blackPlayer != null) {
                blackPlayer.send(message);
            }
            if (whitePlayer != null) {
                whitePlayer.send(message);
            }
        }

        /**
         * Zwraca liczbę kolejnych przejść (pass).
         *
         * @return liczba kolejnych passów
         */
        @Override
        public synchronized int getConsecutivePasses() {
            return consecutivePasses;
        }

        /**
         * Resetuje licznik kolejnych passów do zera.
         */
        @Override
        public synchronized void resetPasses() {
            consecutivePasses = 0;
        }

        /**
         * Zwiększa licznik kolejnych passów o jeden.
         */
        @Override
        public synchronized void incrementPasses() {
            consecutivePasses++;
        }

        /**
         * Zwraca tablicę oznaczeń martwych kamieni stosowaną w fazie liczenia.
         *
         * @return dwuwymiarowa tablica boolean z oznaczeniami
         */
        @Override
        public synchronized boolean[][] getDeadMarks() {
            return deadMarks;
        }

        /**
         * Czyści wszystkie oznaczenia martwych kamieni.
         */
        @Override
        public synchronized void clearDeadMarks() {
            if (deadMarks == null) return;
            for (int r = 0; r < deadMarks.length; r++) {
                for (int c = 0; c < deadMarks[r].length; c++) {
                    deadMarks[r][c] = false;
                }
            }
        }

        /**
         * Przekazuje komendę toggleDead do aktualnego stanu gry.
         *
         * @param row wiersz
         * @param col kolumna
         * @param playerCtx kontekst gracza
         */
        @Override
        public synchronized void toggleDeadCommand(int row, int col, PlayerContext playerCtx) {
            state.toggleDead(this, playerCtx, row, col);
        }

        /**
         * Zmienia oznaczenie martwej grupy na planszy i powiadamia graczy.
         *
         * @param row wiersz
         * @param col kolumna
         * @param playerCtx kontekst gracza wywołującego
         */
        @Override
        public synchronized void toggleDead(int row, int col, PlayerContext playerCtx) {
            try {
                board.toggleDeadGroup(row, col, deadMarks);
                broadcast("MESSAGE [SCORING] Zmieniono status grupy (martwa/zywa).");
                broadcast("BOARD " + boardFlat());
            } catch (IllegalArgumentException e) {
                playerCtx.send("MESSAGE [SCORING] " + e.getMessage());
            }
        }

        /**
         * Zastosowuje oznaczone jako martwe kamienie (usuwa je z planszy).
         */
        @Override
        public synchronized void applyDeadMarks() {
            board.applyDeadMarks(deadMarks);
        }

        /**
         * Wykonuje ruch w kontekście stanu gry i zapisuje go do bazy.
         *
         * @param row wiersz
         * @param col kolumna
         * @param playerCtx gracz wykonujący ruch
         */
        @Override
        public synchronized void makeMove(int row, int col, PlayerContext playerCtx) {
            state.move(this, playerCtx, row, col);
            
            // Zapisujemy tylko udane ruchy (gdy kamień został postawiony)
            if (state instanceof PlayingState) {
                char fieldValue = board.toFlatString().charAt(row * board.getSize() + col);
                if (fieldValue != Board.EMPTY) {
                    saveMoveToDb(row, col, playerCtx.getColor().toString(), "MOVE");
                }
            }
        }

        /**
         * Obsługuje pass wysłany przez gracza i zapisuje go do bazy.
         *
         * @param playerCtx gracz wykonujący pass
         */
        @Override
        public synchronized void pass(PlayerContext playerCtx) {
            state.pass(this, playerCtx);
            saveMoveToDb(-1, -1, playerCtx.getColor().toString(), "PASS");
        }

        /**
         * Obsługuje żądanie wznowienia gry.
         *
         * @param playerCtx gracz żądający wznowienia
         */
        @Override
        public synchronized void requestResume(PlayerContext playerCtx) {
            state.requestResume(this, playerCtx);
        }

        /**
         * Obsługuje zgodę na zakończenie gry (scoring).
         *
         * @param playerCtx gracz akceptujący wynik
         */
        @Override
        public synchronized void agreeEnd(PlayerContext playerCtx) {
            state.agreeEnd(this, playerCtx);
        }

        /**
         * Obsługuje rezygnację gracza i zapisuje ją do bazy.
         *
         * @param playerCtx gracz rezygnujący
         */
        @Override
        public synchronized void resign(PlayerContext playerCtx) {
            state.resign(this, playerCtx);
            saveMoveToDb(-1, -1, playerCtx.getColor().toString(), "RESIGN");
            
            // Określamy zwycięzcę
            String winner = playerCtx.getColor().toString().equals("CZARNY") ? "BIALY" : "CZARNY";
            saveWinner(winner);
        }

        /**
         * Obsługuje opuszczenie gry przez gracza oraz zamyka jego połączenie.
         *
         * @param playerCtx gracz opuszczający grę
         */
        @Override
        public synchronized void quit(PlayerContext playerCtx) {
            // dobrowolne QUIT -> konczymy gre "grzecznie"
            if (!gameEnded) {
                state.quit(this, playerCtx);
                gameEnded = true;
            }

            // zamykamy socket tego gracza
            Player p = (Player) playerCtx;
            p.requestStop();
        }

        /**
         * Obsługuje rozłączenie gracza niezależnie od jego decyzji (disconnect).
         *
         * @param player gracz, który się rozłączył
         */
        private synchronized void onDisconnect(Player player) {
            if (gameEnded) {
                return;
            }

            gameEnded = true;
            setState(new FinishedState());

            Player opponent = player.opponent;
            if (opponent != null) {
                opponent.send("OTHER_PLAYER_LEFT");
            }
        }

        /**
         * Zwraca reprezentację planszy w formacie "flat".
         * W zależności od stanu gry może zawierać oznaczenia martwych kamieni.
         *
         * @return spłaszczony string reprezentujący planszę
         */
        @Override
        public synchronized String boardFlat() {
            if (state != null && state.getName().equals("SCORING")) {
                return board.toFlatStringWithDead(deadMarks);
            }
            return board.toFlatString();
        }

        /**
         * Zwraca rozmiar planszy.
         *
         * @return rozmiar planszy
         */
        @Override
        public int getBoardSize() {
            return board.getSize();
        }
        
        /**
         * Zapisuje ruch do bazy danych.
         *
         * @param r wiersz (-1 dla PASS/RESIGN)
         * @param c kolumna (-1 dla PASS/RESIGN)
         * @param color kolor gracza
         * @param type typ ruchu (MOVE/PASS/RESIGN)
         */
        private void saveMoveToDb(int r, int c, String color, String type) {
            try {
                moveCounter++;
                MoveEntity m = new MoveEntity(dbGameId, moveCounter, r, c, color, type);
                moveRepo.save(m);
                System.out.println("Zapisano ruch #" + moveCounter + " do bazy: " + type + " " + color);
            } catch (Exception e) {
                System.err.println("Błąd zapisu ruchu do bazy: " + e.getMessage());
            }
        }
        
        /**
         * Zapisuje zwycięzcę gry do bazy danych.
         *
         * @param winner zwycięzca (CZARNY/BIALY/REMIS)
         */
        @Override
        public void saveWinner(String winner) {
            try {
                GameEntity entity = gameRepo.findById(dbGameId).orElse(null);
                if (entity != null) {
                    entity.setWinner(winner);
                    gameRepo.save(entity);
                    System.out.println("Zapisano zwycięzcę do bazy: " + winner);
                }
            } catch (Exception e) {
                System.err.println("Błąd zapisu zwycięzcy: " + e.getMessage());
            }
        }

        /**
         * Player reprezentuje połączenie jednego gracza w grze.
         * Obsługuje komunikację z klientem poprzez socket.
         */
        class Player implements Runnable, PlayerContext {
            private final Socket socket;
            private final StoneColor color;
            private Player opponent;
            private BufferedReader in;
            private PrintWriter out;
            private volatile boolean running = true;

            Player(Socket socket, StoneColor color) {
                this.socket = socket;
                this.color = color;
            }

            void setOpponent(Player opponent) {
                this.opponent = opponent;
            }

            @Override
            public void run() {
                CommandParser parser = new CommandParser();

                try {
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);

                    out.println("WELCOME " + color);
                    out.println("BOARD_SIZE " + board.getSize());
                    out.println("BOARD " + boardFlat());

                    if (color == StoneColor.CZARNY) {
                        out.println("MESSAGE Twoj kolor to CZARNY. Ruszasz sie jako pierwszy.");
                        out.println("YOUR_MOVE");
                    } else {
                        out.println("MESSAGE Twoj kolor to BIALY. Czekaj na pierwszy ruch przeciwnika.");
                    }

                    while (running) {
                        String line = in.readLine();
                        if (line == null) {
                            break;
                        }

                        Command command = parser.parse(line);
                        command.execute(Game.this, this);
                    }

                } catch (IOException e) {
                    System.out.println("Player error: " + e.getMessage());
                } finally {
                    try {
                        socket.close();
                    } catch (IOException ignored) {}

                    // Jesli gracz po prostu rozlaczyl sie (bez QUIT/RESIGN/itd.)
                    Game.this.onDisconnect(this);
                }
            }

            /**
             * Zwraca kolor kamieni tego gracza.
             *
             * @return kolor kamieni
             */
            @Override
            public StoneColor getColor() {
                return color;
            }

            /**
             * Zwraca kontekst przeciwnika (jeśli istnieje).
             *
             * @return kontekst przeciwnika
             */
            @Override
            public PlayerContext getOpponent() {
                return opponent;
            }

            /**
             * Wysyła linię tekstu do tego gracza przez socket.
             *
             * @param line wiadomość do wysłania
             */
            @Override
            public void send(String line) {
                if (out != null) {
                    out.println(line);
                }
            }

            /**
             * Żąda zatrzymania wątku obsługującego gracza i zamknięcia socketu.
             */
            @Override
            public void requestStop() {
                running = false;
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }
    }
}