/*
 * Copyright 2016 Johannes Donath <johannesd@torchmind.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rocks.spud.minecraft.mineclock;

import com.google.inject.Guice;
import com.google.inject.Injector;

import javax.annotation.Nonnull;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import rocks.spud.minecraft.mineclock.inject.FXMLProvider;

/**
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class MineClockApplication extends Application {
    private Injector injector;

    public static void main(String[] arguments) {
        launch(MineClockApplication.class, arguments);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(@Nonnull final Stage primaryStage) throws Exception {
        this.injector = Guice.createInjector((b) -> {
            b.bind(MineClockApplication.class).toInstance(this);
            b.bind(Stage.class).toInstance(primaryStage);
            b.bind(FXMLLoader.class).toProvider(FXMLProvider.class);
        });

        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setResizable(false);
        primaryStage.setWidth(960);
        primaryStage.setHeight(540);

        FXMLLoader loader = this.injector.getInstance(FXMLLoader.class);
        Scene scene = new Scene(loader.load(this.getClass().getResourceAsStream("/fxml/MainWindow.fxml")));

        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
