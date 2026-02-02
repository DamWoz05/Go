package pt.training.go.client.gui.view;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import pt.training.go.client.gui.controller.GameController;
import pt.training.go.client.gui.model.ClientUiState;
import pt.training.go.client.gui.net.ClientConnection;
import pt.training.go.client.gui.net.ServerLineListener;
import pt.training.go.client.gui.parser.ServerMessageParser;
import javafx.stage.Modality;
import javafx.beans.binding.Bindings;

//TODO tworzenie serwera za pomoca connectwindow
/**
 * Okno startowe klienta GUI: pozwala podac host i port oraz dolaczyc do serwera.
 *
 * Laczenie odbywa sie w watku w tle.
 * Po udanym polaczeniu okno zostaje zamkniete i otwierane jest glowne okno gry.
 *
 * Dodatkowo:
 * - przy blednym porcie lub braku polaczenia przycisk "Dolacz" miga na czerwono,
 */
public final class ConnectWindow {

    private ConnectWindow() {}

    /**
     * Pokazuje okno laczenia na podanym stage.
     *
     * @param stage stage JavaFX, na ktorym ma byc wyswietlone okno
     */
    public static void show(Stage stage) {
        stage.setTitle("Go - Dołącz");

        TextField hostField = new TextField("localhost");
        TextField portField = new TextField("1988");
        Button joinBtn = new Button("Dołącz");

        hostField.setPrefColumnCount(16);
        portField.setPrefColumnCount(8);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(14));
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Host:"), 0, 0);
        grid.add(hostField, 1, 0);

        grid.add(new Label("Port:"), 0, 1);
        grid.add(portField, 1, 1);

        HBox actions = new HBox(joinBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);
        grid.add(actions, 1, 2);

        joinBtn.setDefaultButton(true);

        joinBtn.setOnAction(e -> {
            String host = hostField.getText().trim();
            int port;

            try {
                port = Integer.parseInt(portField.getText().trim());
                if (port < 1 || port > 65535) throw new NumberFormatException("Port out of range");
            } catch (NumberFormatException ex) {
                flashRed(joinBtn);
                return;
            }

            joinBtn.setDisable(true);

            Thread t = new Thread(() -> tryConnect(stage, host, port, joinBtn), "connect-thread");
            t.setDaemon(true);
            t.start();
        });

        stage.setScene(new Scene(grid));
        stage.setResizable(false);
        stage.show();
    }

    /**
     * Probuje nawiazac polaczenie z serwerem oraz zbudowac "pipeline":
     * ClientConnection -> ServerMessageParser -> GameController -> ClientUiState -> GUI.
     *
     * @param connectStage stage okna laczenia (zostanie zamkniety po sukcesie)
     * @param host host serwera
     * @param port port serwera
     * @param joinBtn przycisk "Dolacz" (do odblokowania i migania przy bledzie)
     */
    private static void tryConnect(Stage connectStage, String host, int port, Button joinBtn) {
        try {
            ClientUiState state = new ClientUiState();
            GameController controller = new GameController(state);
            ServerMessageParser parser = new ServerMessageParser(controller);

            ClientConnection conn = new ClientConnection(host, port, new ServerLineListener() {
                @Override
                public void onLine(String line) {
                    Platform.runLater(() -> parser.parseLine(line));
                }

                @Override
                public void onDisconnected(String reason) {
                    Platform.runLater(() -> state.logs().add("DISCONNECT: " + reason));
                }
            });

            controller.attachConnection(conn);

            Platform.runLater(() -> {
                if (port == 1989) {
                    showReplayLoadDialog(connectStage, conn, controller);
                    return;
                }

                connectStage.close();
                Stage gameStage = new Stage();
                GameWindow.show(gameStage, conn, controller);
                gameStage.setOnCloseRequest(ev -> conn.close());
            });

        } catch (Exception ex) {
            Platform.runLater(() -> {
                joinBtn.setDisable(false);
                flashRed(joinBtn);
            });
        }
    }

    /**
     * Krotka animacja: przycisk miga na czerwono i wraca do poprzedniego stylu.
     *
     * @param btn przycisk do "migniecia"
     */
    private static void flashRed(Button btn) {
        String old = btn.getStyle();
        btn.setStyle(old + "; -fx-background-color: #ff4444; -fx-text-fill: white;");
        PauseTransition pt = new PauseTransition(Duration.millis(180));
        pt.setOnFinished(e -> btn.setStyle(old));
        pt.play();
    }

    private static void showReplayLoadDialog(Stage connectStage, ClientConnection conn, GameController controller) {
        Stage dialog = new Stage();
        dialog.initOwner(connectStage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Replay – wybierz grę");

        Label label = new Label("Podaj ID gry do obejrzenia:");
        TextField idField = new TextField();
        idField.setPromptText("np. 1");

        Button watchBtn = new Button("Obejrzyj");
        Button cancelBtn = new Button("Anuluj");

        watchBtn.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> idField.getText() == null || idField.getText().trim().isEmpty(),
                        idField.textProperty()
                )
        );

        watchBtn.setOnAction(e -> {
            long id;
            try {
                id = Long.parseLong(idField.getText().trim());
                if (id < 0) throw new NumberFormatException("negative");
            } catch (NumberFormatException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Replay");
                a.setHeaderText("Niepoprawne ID");
                a.setContentText("Podaj dodatnią liczbę całkowitą (np. 1).");
                a.showAndWait();
                return;
            }

            conn.sendLine("LOAD " + id);

            dialog.close();
            connectStage.close();

            Stage gameStage = new Stage();
            GameWindow.show(gameStage, conn, controller);
            gameStage.setOnCloseRequest(ev2 -> conn.close());
        });

        cancelBtn.setOnAction(e -> {
            conn.close();
            dialog.close();
        });

        HBox buttons = new HBox(10, watchBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(10, label, idField, buttons);
        root.setPadding(new Insets(12));

        dialog.setScene(new Scene(root, 320, 140));
        dialog.show();
    }
}
