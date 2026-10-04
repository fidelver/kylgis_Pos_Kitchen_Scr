/*
 Chromis POS  - The New Face of Open Source POS
 Copyright (c) 2015 (John Lewis) Chromis.co.uk

 http://www.chromis.co.uk

 kitchen Screen v1.5

 This file is part of chromis & its associated programs

 chromis is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 chromis is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with chromis.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mx.kylgis.kitchenscr;

import java.util.List;
import java.util.Optional;
import javafx.animation.PauseTransition;
import javafx.application.Application;
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
import javafx.scene.layout.StackPane;
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

        try {
            HibernateUtil.getSessionFactory().openSession();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Database Error");
            alert.setHeaderText(null);
            alert.setContentText("Unable to connect to the database.");
            ButtonType buttonOK = new ButtonType("OK");
            alert.getButtonTypes().setAll(buttonOK);
            Optional<ButtonType> result = alert.showAndWait();
            Stage secondaryStage = new Stage();
            Parent root = FXMLLoader.load(getClass().getResource("/com/mx/kylgis/kitchenscr/configuration/database.fxml"));
            secondaryStage.setTitle("Database Configuration - v" + AppLocal.APP_VERSION);
            secondaryStage.setScene(new Scene(root, 600, 500));
            setUserAgentStylesheet(STYLESHEET_MODENA);
            secondaryStage.showAndWait();
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
        KitchenscrController myController = (KitchenscrController) fxmlLoader.getController();

        Scene myScene = new Scene(root, width, height);

        List<Screen> allScreens = Screen.getScreens();
        if ((Boolean.valueOf(AppConfig.getInstance().getProperty("screen.secondscr"))) && (allScreens.size() > 1)) {
            if (allScreens.size() > 1) {
                Screen secondaryScreen = allScreens.get(1);
                javafx.geometry.Rectangle2D bounds = secondaryScreen.getVisualBounds();
                Stage stage = new Stage();
                stage.setX(bounds.getMinX());
                stage.setY(bounds.getMinY());
                stage.setScene(myScene);
                myController.setScene(myScene);
                stage.initStyle(StageStyle.UNDECORATED);
                stage.initModality(Modality.APPLICATION_MODAL);
                publicStage = stage;

            } else {
                Stage stage = new Stage();
                stage.setX(scrXpos);
                stage.setY(scrYpos);
                stage.setScene(myScene);
                myController.setScene(myScene);
                stage.initStyle(StageStyle.UNDECORATED);
                stage.initModality(Modality.APPLICATION_MODAL);
                publicStage = stage;
            }
        } else {
            primaryStage.setTitle("Kitchen Orders");
            primaryStage.setX(scrXpos);
            primaryStage.setY(scrYpos);
            primaryStage.setScene(myScene);
            myController.setScene(myScene);
            primaryStage.initStyle(StageStyle.UNDECORATED);
            publicStage = primaryStage;
        }

        // Aviso de acceso a configuración durante el arranque.
        // KitchenScreen no se muestra hasta que termina este aviso.
        Stage startupConfigStage = new Stage();
        startupConfigStage.setTitle("Kitchen Screen");

        Label startupConfigLabel = new Label(
                "Presiona F12 para entrar a configuración");
        startupConfigLabel.setStyle("-fx-font-size: 16px;");

        StackPane startupConfigPane = new StackPane(startupConfigLabel);
        startupConfigPane.setPrefSize(420, 140);

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
                            "Database Configuration - v" + AppLocal.APP_VERSION);
                    secondaryStage.setScene(
                            new Scene(configRoot, 600, 500));
                    setUserAgentStylesheet(STYLESHEET_MODENA);
                    secondaryStage.initModality(Modality.APPLICATION_MODAL);
                    secondaryStage.showAndWait();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                continueStartup.run();
            }
        });

        startupConfigStage.setOnCloseRequest(event -> {
            if (!configurationRequested[0]) {
                event.consume();
                continueStartup.run();
            }
        });

        startupConfigStage.show();
        startupConfigStage.requestFocus();

        PauseTransition startupConfigTimer =
                new PauseTransition(Duration.seconds(1));

        startupConfigTimer.setOnFinished(event -> {
            if (!configurationRequested[0]) {
                continueStartup.run();
            }
        });

        startupConfigTimer.play();
    }
}
