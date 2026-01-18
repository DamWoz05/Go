package pt.training.go.client.gui.view;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import pt.training.go.client.gui.controller.GameController;
import pt.training.go.client.gui.model.BoardPoint;
import pt.training.go.client.gui.model.ClientUiState;
import pt.training.go.client.gui.model.GamePhase;
import pt.training.go.client.gui.net.ClientConnection;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

/**
 * Glowne okno gry.
 *
 * Okno sklada sie z:
 * - planszy (GoBoardView) z obsluga obserwowania i klikniecia,
 * - panelu logow / historii ruchow (przelaczanie przyciskami),
 * - paska statusu pokazujacego aktualnie wskazywane lub zaznaczone pole,
 * - przyciskow PASS oraz RESIGN.
 *
 * Zachowanie zalezy od fazy gry:
 * - PLAYING: klik wysyla MOVE (tylko w swojej turze), PASS dziala w swojej turze,
 * - SCORING: klik wysyla TOGGLE_DEAD (z potwierdzeniem), PASS daje wybor AGREE_END / REQUEST_RESUME,
 * - FINISHED: akcje sa blokowane, wyswietlany jest dialog z wynikiem po GAME OVER.
 *
 * Dodatkowo:
 * - zaznaczenie pola znika automatycznie po 3 sekundach i wraca tryb obserwowania,
 * - w scoringu, gdy przeciwnik zmieni oznaczenia 'x', GUI pokazuje dialog z pytaniem
 *   czy wznowic gre (REQUEST_RESUME) czy zaakceptowac i kontynuowac scoring.
 */
public final class GameWindow {

    private GameWindow() {}

    /**
     * Tworzy i pokazuje glowne okno gry.
     *
     * @param stage stage JavaFX dla okna gry
     * @param conn aktywne polaczenie do serwera
     * @param controller kontroler obslugujacy logike i wysylanie komend do serwera
     */
    public static void show(Stage stage, ClientConnection conn, GameController controller) {
        ClientUiState state = controller.state();

        final boolean[] gameOverShown = { false };
        final boolean[] localToggleInFlight = { false };
        final boolean[] scoringPromptOpen = { false };

        stage.setTitle("Go - Gra");

        GoBoardView boardView = new GoBoardView();
        boardView.setMinSize(650, 650);

        StackPane boardWrap = new StackPane(boardView);
        boardWrap.setPadding(new Insets(6));
        HBox.setHgrow(boardWrap, Priority.ALWAYS);

        state.boardSizeProperty().addListener((obs, ov, nv) -> {
            int n = nv.intValue();
            boardView.setBoardSize(n);
        });
        state.boardFlatProperty().addListener((obs, ov, nv) -> {
            boardView.setBoardFlat(nv);
        });

        Button btnLogs = new Button("Logi");
        Button btnMoves = new Button("Ruchy");

        btnLogs.setStyle("-fx-background-color: #33a0ff; -fx-text-fill: white;");
        btnMoves.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");

        ListView<String> listView = new ListView<>();
        listView.setItems(state.logs());

        btnLogs.setDisable(true);
        btnMoves.setDisable(false);

        btnLogs.setOnAction(e -> {
            listView.setItems(state.logs());
            btnLogs.setDisable(true);
            btnMoves.setDisable(false);
        });
        btnMoves.setOnAction(e -> {
            listView.setItems(state.moves());
            btnMoves.setDisable(true);
            btnLogs.setDisable(false);
        });

        HBox tabs = new HBox(8, btnLogs, btnMoves);

        Label status = new Label("—");
        status.setMinHeight(44);
        status.setAlignment(Pos.CENTER_LEFT);
        status.setPadding(new Insets(8));
        status.setStyle("-fx-background-color: #b07d5a; -fx-font-size: 16px; -fx-font-weight: bold;");

        Runnable updateStatus = () -> {
            BoardPoint sel = state.selectedProperty().get();
            BoardPoint hov = state.hoverProperty().get();

            if (sel != null) {
                status.setText("Zaznaczono: " + sel.row1() + " " + sel.col1());
                status.setTextFill(Color.LIMEGREEN);
            } else if (hov != null) {
                status.setText("Obserwowane: " + hov.row1() + " " + hov.col1());
                status.setTextFill(Color.YELLOW);
            } else {
                status.setText("—");
                status.setTextFill(Color.WHITE);
            }
        };

        state.hoverProperty().addListener((o,a,b) -> updateStatus.run());
        state.selectedProperty().addListener((o,a,b) -> updateStatus.run());
        updateStatus.run();

        boardView.setOnHover(p -> state.setHover(p));

        PauseTransition clearSelectionTimer = new PauseTransition(Duration.seconds(3));
        clearSelectionTimer.setOnFinished(ev -> {
            state.setSelected(null);
            boardView.setSelected(null);
        });

        boardView.setOnClick(p -> {
            state.setSelected(p);
            boardView.setSelected(p);

            clearSelectionTimer.playFromStart();

            if (p == null) return;
            if (state.getPhase() == GamePhase.FINISHED) return;

            if (state.getPhase() == GamePhase.SCORING) {
                boolean currentlyDead = boardView.isDeadAt(p);

                if (!currentlyDead) {
                    Alert a = new Alert(Alert.AlertType.CONFIRMATION);
                    a.setTitle("Scoring");
                    a.setHeaderText("Oznaczyc grupe jako martwa?");
                    a.setContentText("To oznaczenie zobaczy od razu przeciwnik.\n"
                            + "Jesli sie nie zgodzi, moze wznowic gre (REQUEST_RESUME).");

                    ButtonType yes = new ButtonType("Tak", ButtonBar.ButtonData.YES);
                    ButtonType no = new ButtonType("Nie", ButtonBar.ButtonData.NO);
                    a.getButtonTypes().setAll(yes, no);

                    a.showAndWait().ifPresent(bt -> {
                        if (bt == yes) {
                            localToggleInFlight[0] = true;
                            controller.sendToggleDead(p);
                        }
                    });
                } else {
                    localToggleInFlight[0] = true;
                    controller.sendToggleDead(p);
                }
                return;
            }

            controller.sendMove(p);
        });

        Button btnPass = new Button("PASS");
        Button btnResign = new Button("RESIGN");

        btnPass.setStyle("-fx-background-color: orange; -fx-font-size: 18px; -fx-font-weight: bold;");
        btnResign.setStyle("-fx-background-color: red; -fx-font-size: 18px; -fx-font-weight: bold;");

        btnPass.setMaxWidth(Double.MAX_VALUE);
        btnResign.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnPass, Priority.ALWAYS);
        HBox.setHgrow(btnResign, Priority.ALWAYS);

        btnPass.setOnAction(e -> {
            if (state.getPhase() == GamePhase.FINISHED) return;

            if (state.getPhase() == GamePhase.PLAYING) {
                controller.sendPass();
                return;
            }

            if (state.getPhase() == GamePhase.SCORING) {
                Alert a = new Alert(Alert.AlertType.CONFIRMATION);
                a.setTitle("Scoring");
                a.setHeaderText("Faza ustalania wynikow");
                a.setContentText("Klikaj kamienie, aby oznaczac martwe grupy.\nCo chcesz zrobic?");

                ButtonType agree = new ButtonType("Akceptuj wynik", ButtonBar.ButtonData.OK_DONE);
                ButtonType resume = new ButtonType("Wznow gre", ButtonBar.ButtonData.OTHER);
                ButtonType cancel = new ButtonType("Anuluj", ButtonBar.ButtonData.CANCEL_CLOSE);
                a.getButtonTypes().setAll(agree, resume, cancel);

                a.showAndWait().ifPresent(bt -> {
                    if (bt == agree) controller.sendAgreeEnd();
                    else if (bt == resume) controller.sendRequestResume();
                });
            }
        });

        btnResign.setOnAction(e -> {
            if (state.getPhase() == GamePhase.FINISHED) return;

            Alert a = new Alert(Alert.AlertType.CONFIRMATION);
            a.setTitle("RESIGN");
            a.setHeaderText("Na pewno chcesz poddac gre?");
            a.setContentText("Tej akcji nie da sie cofnac.");

            ButtonType yes = new ButtonType("Tak", ButtonBar.ButtonData.YES);
            ButtonType no = new ButtonType("Nie", ButtonBar.ButtonData.NO);
            a.getButtonTypes().setAll(yes, no);

            a.showAndWait().ifPresent(bt -> {
                if (bt == yes) controller.sendResign();
            });
        });

        btnPass.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> {
                            if (state.getPhase() == GamePhase.FINISHED) return true;
                            if (state.getPhase() == GamePhase.SCORING) return false;
                            return !state.isMyTurn();
                        },
                        state.phaseProperty(),
                        state.myTurnProperty()
                )
        );

        btnResign.disableProperty().bind(
                Bindings.equal(state.phaseProperty(), GamePhase.FINISHED)
        );

        HBox actions = new HBox(12, btnPass, btnResign);

        VBox right = new VBox(10, tabs, listView, status, actions);
        right.setPadding(new Insets(6));
        right.setMinWidth(380);
        right.setPrefWidth(420);
        VBox.setVgrow(listView, Priority.ALWAYS);

        state.lastGameOverLineProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            if (gameOverShown[0]) return;
            gameOverShown[0] = true;

            String verdict = "Koniec gry";
            String winnerLine = newV.replace("[GAME OVER]", "").trim();

            if (state.getMyColor() != null) {
                if (winnerLine.startsWith("WYGRAL CZARNY") && state.getMyColor() == pt.training.go.server.StoneColor.CZARNY) {
                    verdict = "Wygrana";
                } else if (winnerLine.startsWith("WYGRAL BIALY") && state.getMyColor() == pt.training.go.server.StoneColor.BIALY) {
                    verdict = "Wygrana";
                } else if (winnerLine.startsWith("REMIS")) {
                    verdict = "Remis";
                } else {
                    verdict = "Przegrana";
                }
            }

            String body = "";
            if (state.getLastScoreLine() != null) body += state.getLastScoreLine() + "\n\n";
            body += winnerLine;

            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("GAME OVER");
            a.setHeaderText(verdict);
            a.setContentText(body);
            a.showAndWait();
        });

        state.boardFlatProperty().addListener((obs, oldFlat, newFlat) -> {
            if (newFlat == null || oldFlat == null) return;

            if (state.getPhase() != GamePhase.SCORING) return;

            if (!xMarksChanged(oldFlat, newFlat)) return;

            if (localToggleInFlight[0]) {
                localToggleInFlight[0] = false;
                return;
            }

            if (scoringPromptOpen[0]) return;
            scoringPromptOpen[0] = true;

            Alert a = new Alert(Alert.AlertType.CONFIRMATION);
            a.setTitle("Scoring");
            a.setHeaderText("Przeciwnik oznaczyl grupe jako martwa");
            a.setContentText("Jesli sie nie zgadzasz, mozesz wznowic gre.\n"
                    + "Jesli sie zgadzasz, nic nie wysylamy i kontynuujecie oznaczanie.");

            ButtonType ok = new ButtonType("Akceptuje oznaczenie", ButtonBar.ButtonData.OK_DONE);
            ButtonType resume = new ButtonType("Wznow gre", ButtonBar.ButtonData.OTHER);
            ButtonType cancel = new ButtonType("Zamknij", ButtonBar.ButtonData.CANCEL_CLOSE);

            a.getButtonTypes().setAll(ok, resume, cancel);

            a.showAndWait().ifPresent(bt -> {
                if (bt == resume) {
                    controller.sendRequestResume();
                }
            });

            scoringPromptOpen[0] = false;
        });

        HBox root = new HBox(16, boardWrap, right);
        root.setPadding(new Insets(12));

        stage.setScene(new Scene(root, 1100, 650));
        stage.show();

        stage.setOnCloseRequest(e -> conn.close());
    }

    /**
     * Sprawdza czy w danej aktualizacji planszy zmienily sie oznaczenia 'x' (martwe kamienie).
     * Uzywane tylko w fazie SCORING do wykrywania zmian wprowadzonych przez drugiego gracza.
     *
     * @param oldFlat poprzednia plansza
     * @param newFlat nowa plansza
     * @return true jesli zmienil sie przynajmniej jeden znak 'x'
     */
    private static boolean xMarksChanged(String oldFlat, String newFlat) {
        if (oldFlat.length() != newFlat.length()) return true;

        for (int i = 0; i < oldFlat.length(); i++) {
            char a = oldFlat.charAt(i);
            char b = newFlat.charAt(i);

            if (a != b) {
                if (a == 'x' || b == 'x') return true;
            }
        }
        return false;
    }
}
