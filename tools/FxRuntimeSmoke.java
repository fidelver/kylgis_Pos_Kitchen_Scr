import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Screen;
import javafx.stage.Stage;
/** Starts JavaFX without opening POS or connecting to a database. */
public final class FxRuntimeSmoke extends Application {
    @Override public void start(Stage stage) {
        System.out.println("FX_SMOKE_OK screens=" + Screen.getScreens().size());
        Platform.runLater(() -> { stage.close(); Platform.exit(); });
    }
    public static void main(String[] args) { launch(args); }
}
