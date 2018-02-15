/*
 * Copyright 2018 Johannes Donath <johannesd@torchmind.com>
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
package tv.dotstart.minecraft.clock.controller;

import com.google.inject.Injector;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.ResourceBundle;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Rotate;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tv.dotstart.minecraft.clock.service.ConfigurationService;

/**
 * Provides a main application controller which handles the functionality declared in {@code
 * MainWindow.fxml}.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
@Singleton
public class MainWindowController implements Initializable {

  private static final Logger logger = LogManager.getFormatterLogger(MainWindowController.class);

  /**
   * Defines the total amount of time a single day/night cycle takes (assuming that the server is
   * not lagging at the moment).
   */
  private static final Duration CYCLE_TIME = Duration.minutes(20);

  public static final double TIMELINE_POSITION_EVENING = 0.5;
  public static final double TIMELINE_POSITION_MIDNIGHT = 0.75;
  public static final double TIMELINE_POSITION_MORNING = 0;
  public static final double TIMELINE_POSITION_NIGHT = 0.25;

  private final ConfigurationService configurationService;

  private final Rotate cycleRotation = new Rotate(-90, 960, 960);
  private final Timeline cycleTimeline = new Timeline();
  private final Injector injector;

  // <editor-fold desc="FXML Elements">
  @FXML
  private StackPane root;
  @FXML
  private Label time;

  @FXML
  private Label attachmentLabel;
  private Instant attachmentUpdateTime;
  @FXML
  private ImageView backgroundDay;
  @FXML
  private ImageView backgroundEvening;
  @FXML
  private ImageView backgroundMorning;
  @FXML
  private ImageView backgroundNight;
  @FXML
  private HBox controls;
  @FXML
  private ImageView cycle;
  @FXML
  private Button landscapeButton;
  @FXML
  private Button portraitButton;
  @FXML
  private Label rainLabel;
  // </editor-fold>

  @Inject
  public MainWindowController(@Nonnull Injector injector,
      @Nonnull ConfigurationService configurationService) {
    this.injector = injector;
    this.configurationService = configurationService;

    this.cycleTimeline.setCycleCount(Animation.INDEFINITE);
    this.cycleTimeline.getKeyFrames()
        .add(new KeyFrame(CYCLE_TIME, new KeyValue(this.cycleRotation.angleProperty(), 270)));

    this.cycleTimeline.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
      int totalSeconds = (int) (24000 * (newValue.toSeconds() / CYCLE_TIME.toSeconds()));

      int hours = 6 + (totalSeconds / 1000);
      int minutes = (totalSeconds % 1000) / 17;
      boolean pm = false;

      if (hours >= 24) {
        hours %= 24;
      } else if (hours > 12) {
        hours %= 12;
        pm = true;
      } else if (hours == 12 && minutes > 0) {
        pm = true;
      }

      this.time.setText(String.format("%02d:%02d %s", hours, minutes, (pm ? "PM" : "AM")));
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    // Button Switching
    this.portraitButton.managedProperty().bind(this.portraitButton.visibleProperty());
    this.landscapeButton.managedProperty().bind(this.landscapeButton.visibleProperty());

    this.portraitButton.visibleProperty().addListener((ob, o, n) -> {
      if (this.landscapeButton.isVisible() == n) {
        this.landscapeButton.setVisible(!n);
      }
    });

    this.landscapeButton.visibleProperty().addListener((ob, o, n) -> {
      if (this.portraitButton.isVisible() == n) {
        this.portraitButton.setVisible(!n);
      }
    });

    // Morning
    this.cycleTimeline.getKeyFrames().add(
        new KeyFrame(Duration.ZERO, new KeyValue(this.backgroundMorning.opacityProperty(), 1)));
    this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(0.125),
        new KeyValue(this.backgroundMorning.opacityProperty(), 0)));
    this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(0.875),
        new KeyValue(this.backgroundMorning.opacityProperty(), 0)));
    this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(1),
        new KeyValue(this.backgroundMorning.opacityProperty(), 1)));

    // Full Day
    this.cycleTimeline.getKeyFrames()
        .add(new KeyFrame(Duration.ZERO, new KeyValue(this.backgroundDay.opacityProperty(), 0)));
    this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(0.125),
        new KeyValue(this.backgroundDay.opacityProperty(), 1)));
    this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(0.375),
        new KeyValue(this.backgroundDay.opacityProperty(), 1)));
    this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(0.625),
        new KeyValue(this.backgroundDay.opacityProperty(), 0)));

    // Evening
    this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(0.375),
        new KeyValue(this.backgroundEvening.opacityProperty(), 0)));
    this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(0.5),
        new KeyValue(this.backgroundEvening.opacityProperty(), 1)));
    this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(0.625),
        new KeyValue(this.backgroundEvening.opacityProperty(), 0)));

    // Night
    this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(0.5),
        new KeyValue(this.backgroundNight.opacityProperty(), 0)));
    this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(0.625),
        new KeyValue(this.backgroundNight.opacityProperty(), 1)));
    this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(0.875),
        new KeyValue(this.backgroundNight.opacityProperty(), 1)));
    this.cycleTimeline.getKeyFrames()
        .add(new KeyFrame(CYCLE_TIME, new KeyValue(this.backgroundNight.opacityProperty(), 0)));

    // apply transformation
    this.cycle.getTransforms().add(this.cycleRotation);

    this.cycleTimeline.play();

    // Switch to Portrait if requested
    if (this.configurationService.isLaunchPortraitMode()) {
      Platform.runLater(() -> {
        // noinspection ConstantConditions
        this.onPortrait(null);
      });
    }
  }

  /**
   * Sets the cycle time based on a percentage.
   *
   * @param percentage a percentage.
   */
  public void setCycleTime(@Nonnegative double percentage) {
    this.cycleTimeline.jumpTo(CYCLE_TIME.multiply(percentage));
  }

  // <editor-fold desc="Event Handlers">

  /**
   * Switches the application into landscape mode.
   */
  @FXML
  private void onLandscape(@Nonnull ActionEvent event) {
    logger.info("Switching to landscape mode");

    this.root.getScene().getWindow().setWidth(960);
    this.root.getStyleClass().remove("portrait");

    this.landscapeButton.setVisible(false);
  }

  /**
   * Switches the application to portrait mode.
   */
  @FXML
  private void onPortrait(@Nonnull ActionEvent event) {
    logger.info("Switching to portrait mode");

    this.root.getScene().getWindow().setWidth(400);
    this.root.getStyleClass().add("portrait");

    this.portraitButton.setVisible(false);
  }

  /**
   * Adjusts the time to the predefined evening time (e.g. half of the day/night cycle has been
   * completed).
   */
  @FXML
  private void onSetEvening(@Nonnull ActionEvent event) {
    logger.info("Manually switching time to evening");
    this.setCycleTime(TIMELINE_POSITION_EVENING);
  }

  /**
   * Adjusts the time to the predefined midnight time (e.g. 75% of the day/night cycle has been
   * completed).
   */
  @FXML
  private void onSetMidnight(@Nonnull ActionEvent event) {
    logger.info("Manually switching time to midnight");
    this.setCycleTime(TIMELINE_POSITION_MIDNIGHT);
  }

  /**
   * Adjusts the time to the predefined sunrise time (e.g. 0% of the day/night cycle has been
   * completed).
   */
  @FXML
  private void onSetMorning(@Nonnull ActionEvent event) {
    logger.info("Manually switching time to morning");
    this.setCycleTime(TIMELINE_POSITION_MORNING);
  }

  /**
   * Adjusts the time to the predefined noon time (e.g. 25% of the day/night cycle has been
   * completed).
   */
  @FXML
  private void onSetNoon(@Nonnull ActionEvent event) {
    logger.info("Manually switching time to noon");
    this.setCycleTime(TIMELINE_POSITION_NIGHT);
  }

  /**
   * Opens a settings dialogue which permits the customization of the application.
   */
  @FXML
  private void onSettings(@Nonnull ActionEvent event) {
    logger.info("Opening settings dialogue");

    try {
      Scene scene = new Scene(this.injector.getInstance(FXMLLoader.class)
          .load(this.getClass().getResourceAsStream("/fxml/SettingsWindow.fxml")));

      Stage stage = new Stage(StageStyle.UNDECORATED);
      stage.initOwner(this.root.getScene().getWindow());
      stage.initModality(Modality.WINDOW_MODAL);
      stage.setScene(scene);
      stage.setWidth(500);
      stage.setHeight(500);

      stage.show();
    } catch (IOException ex) {
      throw new RuntimeException("Could not access settings window: " + ex.getMessage(), ex);
    }
  }
  // </editor-fold>
}
