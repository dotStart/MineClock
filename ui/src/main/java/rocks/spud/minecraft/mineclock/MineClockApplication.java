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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javax.annotation.Nonnull;
import rocks.spud.minecraft.mineclock.inject.FXMLProvider;

/**
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class MineClockApplication extends Application {

  private Injector injector;

  /**
   * Retrieves the path to a systems specific storage directory.
   *
   * @return a storage directory.
   */
  @Nonnull
  public static Path getApplicationDirectory() {
    Path basePath = null;

    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      String applicationDataDirectory = System.getenv("APPDATA");

      if (applicationDataDirectory != null) {
        basePath = Paths.get(applicationDataDirectory, ".mineclock");
      }
    }

    if (basePath == null) {
      basePath = Paths.get(System.getProperty("java.home"), ".mineclock");
    }

    try {
      Files.createDirectories(basePath);
    } catch (IOException ex) {
      throw new RuntimeException(
          "Could not create application storage directory: " + ex.getMessage(), ex);
    }

    return basePath;
  }

  public static void main(String[] arguments) {
    launch(MineClockApplication.class, arguments);
  }

  /**
   * Displays a dialog which reports an unexpected exception to a user in a way that allows them to
   * easily report it.
   */
  public static void reportError(@Nonnull Throwable throwable) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.initModality(Modality.APPLICATION_MODAL);

    alert.setTitle("Application Error");
    alert.setHeaderText("Application Error");
    alert.setContentText(
        "An unexpected condition occurred and the requested action has not completed successfully. This is a bug, please report it as such.");

    // Create expandable Exception.
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    throwable.printStackTrace(pw);
    String exceptionText = sw.toString();

    TextArea textArea = new TextArea(exceptionText);
    textArea.setEditable(false);
    textArea.setWrapText(true);

    textArea.setMaxWidth(Double.MAX_VALUE);
    textArea.setMaxHeight(Double.MAX_VALUE);
    GridPane.setVgrow(textArea, Priority.ALWAYS);
    GridPane.setHgrow(textArea, Priority.ALWAYS);

    GridPane expContent = new GridPane();
    expContent.setMaxWidth(Double.MAX_VALUE);
    expContent.add(textArea, 0, 1);

    alert.getDialogPane().setExpandableContent(expContent);
    throwable.printStackTrace();
    alert.showAndWait();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start(@Nonnull final Stage primaryStage) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
      reportError(e);
      System.exit(-1);
    });

    primaryStage.getIcons()
        .add(new Image(this.getClass().getResourceAsStream("/image/application.png")));

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
    Scene scene = new Scene(
        loader.load(this.getClass().getResourceAsStream("/fxml/MainWindow.fxml")));

    primaryStage.setScene(scene);
    primaryStage.show();
  }
}
