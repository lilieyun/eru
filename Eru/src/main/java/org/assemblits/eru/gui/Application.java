/******************************************************************************
 * Copyright (c) 2017 Assemblits contributors                                 *
 *                                                                            *
 * This file is part of Eru The open JavaFX SCADA by Assemblits Organization. *
 *                                                                            *
 * Eru The open JavaFX SCADA is free software: you can redistribute it        *
 * and/or modify it under the terms of the GNU General Public License         *
 *  as published by the Free Software Foundation, either version 3            *
 *  of the License, or (at your option) any later version.                    *
 *                                                                            *
 * Eru is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.            *
 ******************************************************************************/
package org.assemblits.eru.gui;

import javafx.application.Preloader;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.assemblits.eru.gui.component.StartUpWizard;
import org.assemblits.eru.gui.controller.EruController;
import org.assemblits.eru.gui.controller.EruPreloaderController;
import org.assemblits.eru.gui.service.ApplicationArgsPreparer;
import org.assemblits.eru.gui.service.ApplicationLoader;
import org.assemblits.eru.preferences.EruPreferences;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import static com.sun.javafx.application.LauncherImpl.launchApplication;

@Slf4j
@SpringBootApplication
@ComponentScan("org.assemblits.eru")
public class Application extends javafx.application.Application {

    private ConfigurableApplicationContext applicationContext;
    @Autowired
    private EruController eruController;

    public static void main(String[] args) {
        launchApplication(Application.class, args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        final EruPreferences eruPreferences = new EruPreferences();
        if (!eruPreferences.getApplicationConfigured().getValue()) {
            StartUpWizard startUpWizard = new StartUpWizard(stage, eruPreferences);
            startUpWizard.startWizard();
        }

        ApplicationLoader applicationLoader = new ApplicationLoader(this, getClass(), getApplicationParameters());
        Preloader preloaderWindow = loadService(applicationLoader);
        applicationLoader.setOnSucceeded(event -> {
            applicationContext = (ConfigurableApplicationContext) event.getSource().getValue();
            eruController.startEru(stage);
        });

        preloaderWindow.start(stage);
        applicationLoader.start();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        applicationContext.close();
    }

    private Preloader loadService(ApplicationLoader applicationLoader) {
        // TODO: Set Eru Icon to the preloader stage
        return new Preloader() {
            @Override
            public void start(Stage primaryStage) throws Exception {
                FXMLLoader loader = new FXMLLoader();
                loader.setController(new EruPreloaderController(applicationLoader));
                loader.setLocation(getClass().getResource("/views/Preloader.fxml"));
                Parent preLoader = loader.load();

                primaryStage.setScene(new Scene(preLoader));
                primaryStage.show();
            }
        };
    }

    private String[] getApplicationParameters() {
        ApplicationArgsPreparer environmentPreparer = new ApplicationArgsPreparer();
        final Parameters parametersObject = getParameters();
        return environmentPreparer.prepare(parametersObject.getRaw().toArray(new String[0]));
    }

    public enum Theme {
        DEFAULT, DARK;
        public String getStyleSheetURL(){
            return "/views/styles/" + name() + ".css";
        }
    }

}
