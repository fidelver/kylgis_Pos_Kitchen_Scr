/*
 KylGis Kitchen Screen
 Modifications Copyright (c) 2026 KylGis
 Portions Copyright (c) 2015 John Lewis / Chromis

 Based on Chromis Kitchen Screen. Upstream attribution is retained under the
 GNU General Public License, version 3 or (at your option) any later version.

 KylGis Kitchen Screen is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by the
 Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 KylGis Kitchen Screen is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with KylGis Kitchen Screen. If not, see <http://www.gnu.org/licenses/>.
 */

package com.mx.kylgis.kitchenscr.configuration;

import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.mx.kylgis.kitchenscr.forms.AppLocal;

/**
 *
 * @author John
 */
public class Configuration extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        Parent root = FXMLLoader.load(getClass().getResource("database.fxml"));      
        primaryStage.setTitle("Database Configuration - v" + AppLocal.APP_VERSION);
        primaryStage.setScene(new Scene(root, 600, 500));
        setUserAgentStylesheet(STYLESHEET_MODENA);

        primaryStage.show();

    }

}
