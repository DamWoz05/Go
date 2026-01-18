package pt.training.go.client.gui.app;

import javafx.application.Application;
import javafx.stage.Stage;
import pt.training.go.client.gui.view.ConnectWindow;

/**
 * Główna klasa startowa aplikacji klienta GUI (JavaFX).
 */
public class GuiClientApp extends Application {

    /**
     * Punkt startu JavaFX. Tworzy i pokazuje pierwsze okno aplikacji.
     *
     * @param primaryStage główny stage dostarczony przez JavaFX runtime
     */
    @Override
    public void start(Stage primaryStage) {
        ConnectWindow.show(primaryStage);
    }
}
