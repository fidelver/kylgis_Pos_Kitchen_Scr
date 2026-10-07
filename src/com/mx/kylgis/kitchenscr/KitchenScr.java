/*
 KylGis POS Monitor de Cocina
 Modifications Copyright (c) 2026 KylGis
 Portions Copyright (c) 2015 John Lewis / Chromis

 Based on Chromis Kitchen Screen. Upstream attribution is retained under the
 GNU General Public License, version 3 or (at your option) any later version.

 KylGis POS Monitor de Cocina is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by the
 Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 KylGis POS Monitor de Cocina is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with KylGis POS Monitor de Cocina. If not, see <http://www.gnu.org/licenses/>.
 */
package com.mx.kylgis.kitchenscr;

import java.util.List;
import java.util.Optional;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import static javafx.application.Application.STYLESHEET_MODENA;
import static javafx.application.Application.setUserAgentStylesheet;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import com.mx.kylgis.kitchenscr.forms.AppConfig;
import com.mx.kylgis.kitchenscr.forms.AppLocal;
import com.mx.kylgis.kitchenscr.hibernate.HibernateUtil;

/**
 *
 * @author John Lewis 2015
 */
public class KitchenScr extends Application {

    private int width = 1024;
    private int height = 768;
    private int scrXpos = 0;
    private int scrYpos = 0;

    public static String parameter;
    public static Stage publicStage;
    private KitchenscrController kitchenController;
    private boolean cleanupPerformed = false;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length != 0) {
            parameter = args[0];
        }
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        boolean databaseReady = HibernateUtil.testConnection();
        if (!databaseReady) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Error de base de datos");
            alert.setHeaderText(null);
            alert.setContentText("No fue posible conectar con la base de datos.");
            ButtonType buttonOK = new ButtonType("Aceptar");
            alert.getButtonTypes().setAll(buttonOK);
            alert.showAndWait();

            Stage secondaryStage = new Stage();
            Parent root = FXMLLoader.load(getClass().getResource(
                    "/com/mx/kylgis/kitchenscr/configuration/database.fxml"));
            secondaryStage.setTitle(AppLocal.APP_NAME
                    + " - Configuración de base de datos - v" + AppLocal.APP_VERSION);
            applyBrandIcon(secondaryStage);
            secondaryStage.setScene(new Scene(root, 600, 500));
            setUserAgentStylesheet(STYLESHEET_MODENA);
            installApplicationCloseHandler(secondaryStage);
            secondaryStage.showAndWait();

            databaseReady = HibernateUtil.rebuildSessionFactory();
            if (!databaseReady) {
                Alert retryAlert = new Alert(Alert.AlertType.ERROR);
                retryAlert.setTitle("Base de datos no disponible");
                retryAlert.setHeaderText(null);
                retryAlert.setContentText(
                        "La conexión sigue sin estar disponible. El Monitor de Cocina se cerrará sin cargar las comandas.");
                retryAlert.showAndWait();
                Platform.exit();
                return;
            }
        }

        try {
            if (AppConfig.getInstance().getProperty("screen.width") != null) {
                width = Integer.parseInt(AppConfig.getInstance().getProperty("screen.width"));
            }
            if (AppConfig.getInstance().getProperty("screen.height") != null) {
                height = Integer.parseInt(AppConfig.getInstance().getProperty("screen.height"));
            }
        } catch (IllegalArgumentException e) {
            width = 1024;
            height = 768;
        }

        try {
            if (AppConfig.getInstance().getProperty("screen.width") != null) {
                width = Integer.parseInt(AppConfig.getInstance().getProperty("screen.width"));
            }
            if (AppConfig.getInstance().getProperty("screen.height") != null) {
                height = Integer.parseInt(AppConfig.getInstance().getProperty("screen.height"));
            }
        } catch (IllegalArgumentException e) {
            width = 1024;
            height = 768;
        }

        try {
            if (AppConfig.getInstance().getProperty("screen.xpos") != null) {
                width = Integer.parseInt(AppConfig.getInstance().getProperty("screen.width"));
            }
            if (AppConfig.getInstance().getProperty("screen.ypos") != null) {
                height = Integer.parseInt(AppConfig.getInstance().getProperty("screen.height"));
            }
        } catch (IllegalArgumentException e) {
            scrXpos = 0;
            scrYpos = 0;
        }

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("kitchenscr.fxml"));
        Parent root = (Parent) fxmlLoader.load();
        kitchenController = (KitchenscrController) fxmlLoader.getController();

        Scene myScene = new Scene(root, width, height);

        List<Screen> allScreens = Screen.getScreens();
        if ((Boolean.valueOf(AppConfig.getInstance().getProperty("screen.secondscr"))) && (allScreens.size() > 1)) {
            if (allScreens.size() > 1) {
                Screen secondaryScreen = allScreens.get(1);
                javafx.geometry.Rectangle2D bounds = secondaryScreen.getVisualBounds();
                Stage stage = new Stage();
                stage.setTitle(AppLocal.APP_NAME);
                stage.setX(bounds.getMinX());
                stage.setY(bounds.getMinY());
                stage.setScene(myScene);
                applyBrandIcon(stage);
                kitchenController.setScene(myScene);
                stage.initStyle(StageStyle.UNDECORATED);
                stage.initModality(Modality.APPLICATION_MODAL);
                publicStage = stage;

            } else {
                Stage stage = new Stage();
                stage.setTitle(AppLocal.APP_NAME);
                stage.setX(scrXpos);
                stage.setY(scrYpos);
                stage.setScene(myScene);
                applyBrandIcon(stage);
                kitchenController.setScene(myScene);
                stage.initStyle(StageStyle.UNDECORATED);
                stage.initModality(Modality.APPLICATION_MODAL);
                publicStage = stage;
            }
        } else {
            primaryStage.setTitle(AppLocal.APP_NAME);
            applyBrandIcon(primaryStage);
            primaryStage.setX(scrXpos);
            primaryStage.setY(scrYpos);
            primaryStage.setScene(myScene);
            kitchenController.setScene(myScene);
            primaryStage.initStyle(StageStyle.UNDECORATED);
            publicStage = primaryStage;
        }

        installApplicationCloseHandler(publicStage);

        // Aviso de acceso a configuración durante el arranque.
        // KitchenScreen no se muestra hasta que termina este aviso.
        Stage startupConfigStage = new Stage();
        startupConfigStage.setTitle(AppLocal.APP_NAME);
        applyBrandIcon(startupConfigStage);

        ImageView startupLogo = new ImageView(new Image(
                getClass().getResourceAsStream(
                        "/com/mx/kylgis/kitchenscr/images/kylgis_main.png")));
        startupLogo.setFitWidth(340);
        startupLogo.setPreserveRatio(true);
        startupLogo.setSmooth(true);

        Label startupConfigLabel = new Label("F12 Conf.");
        startupConfigLabel.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: #666666;");

        VBox startupConfigPane = new VBox(8, startupLogo, startupConfigLabel);
        startupConfigPane.setAlignment(Pos.CENTER);
        startupConfigPane.setPrefSize(440, 235);
        startupConfigPane.setStyle("-fx-background-color: white;");

        Scene startupConfigScene = new Scene(startupConfigPane);

        startupConfigStage.setScene(startupConfigScene);
        startupConfigStage.setResizable(false);

        final boolean[] configurationRequested = {false};
        final boolean[] startupFinished = {false};

        Runnable continueStartup = () -> {
            if (!startupFinished[0]) {
                startupFinished[0] = true;

                if (startupConfigStage.isShowing()) {
                    startupConfigStage.close();
                }

                publicStage.show();
            }
        };

        startupConfigScene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.F12) {
                configurationRequested[0] = true;
                event.consume();

                startupConfigStage.close();

                try {
                    Stage secondaryStage = new Stage();
                    Parent configRoot = FXMLLoader.load(
                            getClass().getResource(
                                    "/com/mx/kylgis/kitchenscr/configuration/database.fxml"));
                    secondaryStage.setTitle(
                            AppLocal.APP_NAME + " - Configuración de base de datos - v" + AppLocal.APP_VERSION);
                    applyBrandIcon(secondaryStage);
                    secondaryStage.setScene(
                            new Scene(configRoot, 600, 500));
                    setUserAgentStylesheet(STYLESHEET_MODENA);
                    secondaryStage.initModality(Modality.APPLICATION_MODAL);
                    installApplicationCloseHandler(secondaryStage);
                    secondaryStage.showAndWait();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                continueStartup.run();
            }
        });

        startupConfigStage.setOnCloseRequest(event -> {
            event.consume();
            shutdownApplication();
        });

        startupConfigStage.show();
        startupConfigStage.requestFocus();

        PauseTransition startupConfigTimer =
                new PauseTransition(Duration.seconds(2));

        startupConfigTimer.setOnFinished(event -> {
            if (!configurationRequested[0]) {
                continueStartup.run();
            }
        });

        startupConfigTimer.play();
    }
    private void installApplicationCloseHandler(Stage stage) {
        if (stage == null) {
            return;
        }
        stage.setOnCloseRequest(event -> {
            event.consume();
            shutdownApplication();
        });
    }

    private void cleanupApplication() {
        if (cleanupPerformed) {
            return;
        }
        cleanupPerformed = true;

        try {
            if (kitchenController != null) {
                kitchenController.shutdown();
            }
        } catch (Exception ignored) {
            // Continue closing the application.
        }

        try {
            HibernateUtil.shutdown();
        } catch (Exception ignored) {
            // Continue closing the application.
        }
    }

    private void shutdownApplication() {
        cleanupApplication();
        Platform.exit();
        System.exit(0);
    }

    @Override
    public void stop() {
        cleanupApplication();
    }

    private void applyBrandIcon(Stage stage) {
        try {
            java.io.InputStream iconStream = getClass().getResourceAsStream(
                    "/com/mx/kylgis/kitchenscr/images/kylgis_icon.png");
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
                iconStream.close();
            }
        } catch (Exception ignored) {
            // Branding icon must never prevent Kitchen Screen from starting.
        }
    }
}
