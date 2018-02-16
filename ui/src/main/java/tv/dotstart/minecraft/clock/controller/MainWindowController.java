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
import java.util.Timer;
import java.util.TimerTask;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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

  /**
   * Defines the total amount of time that has to pass between now and the last synchronization
   * packet before the application no longer considers itself to be application controlled.
   */
  private static final java.time.Duration SYNCHRONIZATION_EXPIRATION_DURATION = java.time.Duration
      .ofMinutes(1);

  /**
   * Defines the total amount of time the application uses to animate the transition between
   * different states.
   */
  private static final Duration TRANSITION_DURATION = Duration.seconds(2);

  public static final double TIMELINE_POSITION_EVENING = 0.5;
  public static final double TIMELINE_POSITION_MIDNIGHT = 0.75;
  public static final double TIMELINE_POSITION_MORNING = 0;
  public static final double TIMELINE_POSITION_NIGHT = 0.25;

  private final Injector injector;
  private final ConfigurationService configurationService;

  private final Rotate cycleRotation = new Rotate(-90, 960, 960);
  private final Timeline cycleTimeline = new Timeline();

  private final BooleanProperty raining = new SimpleBooleanProperty();
  private BooleanBinding rainBinding;
  private FadeTransition rainTransition;

  private final Timer synchronizationTimer = new Timer(true);
  private Instant lastSynchronizationTimestamp = Instant.EPOCH;

  // <editor-fold desc="FXML Elements">
  @FXML
  private StackPane root;
  @FXML
  private Label time;

  @FXML
  private Label synchronizationLabel;
  @FXML
  private ImageView backgroundDay;
  @FXML
  private ImageView backgroundEvening;
  @FXML
  private ImageView backgroundMorning;
  @FXML
  private ImageView backgroundNight;
  @FXML
  private ImageView backgroundRain;
  @FXML
  private HBox controls;
  @FXML
  private ImageView cycle;
  @FXML
  private Button landscapeButton;
  @FXML
  private Button portraitButton;
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

    this.synchronizationTimer.schedule(new SynchronizationTask(), 1000, 2000);
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

    // rain transition
    this.rainTransition = new FadeTransition(TRANSITION_DURATION, this.backgroundRain);
    this.rainBinding = this.configurationService.displayWeatherProperty().and(this.raining);
    this.rainBinding.addListener((observable, oldValue, newValue) -> {
      this.rainTransition.setFromValue(newValue ? 0 : 1);
      this.rainTransition.setToValue(newValue ? 1 : 0);

      this.rainTransition.play();
    });

    // Switch to Portrait if requested
    if (this.configurationService.isLaunchPortraitMode()) {
      Platform.runLater(() -> {
        // noinspection ConstantConditions
        this.onPortrait(null);
      });
    }
  }

  /**
   * Refreshes the current synchronization state.
   */
  public void refreshSynchronization() {
    this.lastSynchronizationTimestamp = Instant.now();

    if (this.controls.getOpacity() == 1.0) {
      FadeTransition fadeOutTransition = new FadeTransition(TRANSITION_DURATION, this.controls);
      fadeOutTransition.setFromValue(1.0);
      fadeOutTransition.setToValue(0.0);
      fadeOutTransition.play();

      FadeTransition fadeInTransition = new FadeTransition(TRANSITION_DURATION,
          this.synchronizationLabel);
      fadeInTransition.setFromValue(0.0);
      fadeInTransition.setToValue(1.0);
      fadeInTransition.play();
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

  /**
   * Sets whether it is currently raining.
   *
   * @param raining true if raining, false otherwise.
   */
  public void setRaining(boolean raining) {
    this.raining.set(raining);
  }

  /**
   * Sets whether the game is currently paused (internal server only).
   *
   * @param paused true if game is paused, false otherwise.
   */
  public void setPaused(boolean paused) {
    if (paused) {
      this.cycleTimeline.pause();
    } else {
      this.cycleTimeline.play();
    }
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

  /**
   * Re-Evaluates the synchronization state of the application on a regular basis.
   */
  private class SynchronizationTask extends TimerTask {

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      Platform.runLater(() -> {

        Instant expirationTimestamp = MainWindowController.this.lastSynchronizationTimestamp
            .plus(SYNCHRONIZATION_EXPIRATION_DURATION);

        if (expirationTimestamp.isBefore(Instant.now())) {
          // reset world state information which would otherwise not reset on its own (e.g.
          // functionality which is specific to the synchronization server)
          MainWindowController.this.setRaining(false);
          MainWindowController.this.setPaused(false);

          if (MainWindowController.this.synchronizationLabel.getOpacity() == 1.0) {
            FadeTransition fadeOutTransition = new FadeTransition(TRANSITION_DURATION,
                MainWindowController.this.synchronizationLabel);
            fadeOutTransition.setFromValue(1.0);
            fadeOutTransition.setToValue(0.0);
            fadeOutTransition.play();
          }

          if (MainWindowController.this.controls.getOpacity() == 0.0) {
            FadeTransition fadeInTransition = new FadeTransition(TRANSITION_DURATION,
                MainWindowController.this.controls);
            fadeInTransition.setFromValue(0.0);
            fadeInTransition.setToValue(1.0);
            fadeInTransition.play();
          }
        }
      });
    }
  }
  // </editor-fold>
}
