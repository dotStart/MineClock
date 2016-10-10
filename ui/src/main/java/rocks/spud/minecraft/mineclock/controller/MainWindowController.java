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
package rocks.spud.minecraft.mineclock.controller;

import java.net.URL;
import java.time.Instant;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;
import rocks.spud.minecraft.mineclock.attach.MinecraftAttachment;
import rocks.spud.minecraft.mineclock.attach.agent.common.ClockMessage;
import rocks.spud.minecraft.mineclock.attach.agent.common.ClockProtocol;

/**
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class MainWindowController implements Initializable {
    private static final Duration CYCLE_TIME = Duration.minutes(20);
    private static final java.time.Duration ATTACHMENT_EXPIRATION_DURATION = java.time.Duration.ofSeconds(20);

    private final Rotate cycleRotation = new Rotate(-90, 960, 960);
    private final Timeline cycleTimeline = new Timeline();

    private final ClockProtocol protocol;
    private final MinecraftAttachment minecraftAttachment;
    private final Timer attachmentTimer;
    private Instant attachmentUpdateTime;

    @FXML
    private StackPane root;
    @FXML
    private Button portraitButton;
    @FXML
    private Button landscapeButton;

    @FXML
    private ImageView cycle;
    @FXML
    private ImageView backgroundDay;
    @FXML
    private ImageView backgroundEvening;
    @FXML
    private ImageView backgroundMorning;
    @FXML
    private ImageView backgroundNight;

    @FXML
    private Label time;
    @FXML
    private Label rainLabel;

    @FXML
    private HBox controls;
    @FXML
    private Label attachmentLabel;

    private double initialX;
    private double initialY;

    public MainWindowController() {
        this.cycleTimeline.setCycleCount(Animation.INDEFINITE);
        this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME, new KeyValue(this.cycleRotation.angleProperty(), 270)));

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

        if (MinecraftAttachment.isAvailable()) {
            this.protocol = new ClockProtocol(this::onClockUpdate);
            this.protocol.listen();

            this.minecraftAttachment = MinecraftAttachment.getAttachment();

            this.attachmentTimer = new Timer(true);
            this.attachmentTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> {
                        if (attachmentUpdateTime != null && attachmentUpdateTime.plus(ATTACHMENT_EXPIRATION_DURATION).isBefore(Instant.now())) {
                            if (attachmentLabel.getOpacity() == 1.0) {
                                FadeTransition fadeOutTransition = new FadeTransition(Duration.seconds(2), attachmentLabel);
                                fadeOutTransition.setFromValue(1.0);
                                fadeOutTransition.setToValue(0.0);
                                fadeOutTransition.play();
                            }

                            if (rainLabel.getOpacity() == 1.0) {
                                FadeTransition rainFadeOutTransition = new FadeTransition(Duration.seconds(2), rainLabel);
                                rainFadeOutTransition.setFromValue(1.0);
                                rainFadeOutTransition.setToValue(0.0);
                                rainFadeOutTransition.play();
                            }

                            if (controls.getOpacity() == 0.0) {
                                FadeTransition fadeInTransition = new FadeTransition(Duration.seconds(2), controls);
                                fadeInTransition.setFromValue(0.0);
                                fadeInTransition.setToValue(1.0);
                                fadeInTransition.play();
                            }
                        }
                    });

                    minecraftAttachment.refresh();
                }
            }, 1000, 2000);
        } else {
            this.protocol = null;
            this.minecraftAttachment = null;
            this.attachmentTimer = null;
        }
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
        this.cycleTimeline.getKeyFrames().add(new KeyFrame(Duration.ZERO, new KeyValue(this.backgroundMorning.opacityProperty(), 1)));
        this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(0.125), new KeyValue(this.backgroundMorning.opacityProperty(), 0)));
        this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(0.875), new KeyValue(this.backgroundMorning.opacityProperty(), 0)));
        this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(1), new KeyValue(this.backgroundMorning.opacityProperty(), 1)));

        // Full Day
        this.cycleTimeline.getKeyFrames().add(new KeyFrame(Duration.ZERO, new KeyValue(this.backgroundDay.opacityProperty(), 0)));
        this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(0.125), new KeyValue(this.backgroundDay.opacityProperty(), 1)));
        this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(0.375), new KeyValue(this.backgroundDay.opacityProperty(), 1)));
        this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(0.625), new KeyValue(this.backgroundDay.opacityProperty(), 0)));

        // Evening
        this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(0.375), new KeyValue(this.backgroundEvening.opacityProperty(), 0)));
        this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(0.5), new KeyValue(this.backgroundEvening.opacityProperty(), 1)));
        this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(0.625), new KeyValue(this.backgroundEvening.opacityProperty(), 0)));

        // Night
        this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(0.5), new KeyValue(this.backgroundNight.opacityProperty(), 0)));
        this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(0.625), new KeyValue(this.backgroundNight.opacityProperty(), 1)));
        this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME.multiply(0.875), new KeyValue(this.backgroundNight.opacityProperty(), 1)));
        this.cycleTimeline.getKeyFrames().add(new KeyFrame(CYCLE_TIME, new KeyValue(this.backgroundNight.opacityProperty(), 0)));

        // apply transformation
        this.cycle.getTransforms().add(this.cycleRotation);

        this.cycleTimeline.play();
    }

    /**
     * Sets the cycle time based on a percentage.
     *
     * @param percentage a percentage.
     */
    private void setCycleTime(@Nonnegative double percentage) {
        this.cycleTimeline.jumpTo(CYCLE_TIME.multiply(percentage));
    }

    @FXML
    private void onPortrait(@Nonnull ActionEvent event) {
        this.root.getScene().getWindow().setWidth(400);
        this.root.getStyleClass().add("portrait");

        this.portraitButton.setVisible(false);
    }

    @FXML
    private void onLandscape(@Nonnull ActionEvent event) {
        this.root.getScene().getWindow().setWidth(960);
        this.root.getStyleClass().remove("portrait");

        this.landscapeButton.setVisible(false);
    }

    @FXML
    private void onIconify(@Nonnull ActionEvent event) {
        ((Stage) this.root.getScene().getWindow()).setIconified(true);
    }

    @FXML
    private void onTitleBarMousePressed(@Nonnull MouseEvent event) {
        if (event.getButton() != MouseButton.MIDDLE) {
            initialX = event.getSceneX();
            initialY = event.getSceneY();
        } else {
            this.root.getScene().getWindow().centerOnScreen();
            initialX = this.root.getScene().getWindow().getX();
            initialY = this.root.getScene().getWindow().getY();
        }
    }

    @FXML
    private void onTitleBarMouseDragged(@Nonnull MouseEvent event) {
        if (event.getButton() != MouseButton.MIDDLE) {
            this.root.getScene().getWindow().setX(event.getScreenX() - initialX);
            this.root.getScene().getWindow().setY(event.getScreenY() - initialY);
        }
    }

    @FXML
    private void onClose(@Nonnull ActionEvent event) {
        Platform.exit();
    }

    @FXML
    private void onSetMorning(@Nonnull ActionEvent event) {
        this.setCycleTime(0);
    }

    @FXML
    private void onSetNoon(@Nonnull ActionEvent event) {
        this.setCycleTime(0.25);
    }

    @FXML
    private void onSetEvening(@Nonnull ActionEvent event) {
        this.setCycleTime(0.5);
    }

    @FXML
    private void onSetMidnight(@Nonnull ActionEvent event) {
        this.setCycleTime(0.75);
    }

    /**
     * Handles clock updates from an attached virtual machine.
     */
    private void onClockUpdate(@Nonnull final ClockMessage message) {
        Platform.runLater(() -> {
            this.attachmentUpdateTime = Instant.now();

            if (this.controls.getOpacity() == 1.0) {
                FadeTransition fadeOutTransition = new FadeTransition(Duration.seconds(2), this.controls);
                fadeOutTransition.setFromValue(1.0);
                fadeOutTransition.setToValue(0.0);
                fadeOutTransition.play();

                FadeTransition fadeInTransition = new FadeTransition(Duration.seconds(2), this.attachmentLabel);
                fadeInTransition.setFromValue(0.0);
                fadeInTransition.setToValue(1.0);
                fadeInTransition.play();
            }

            if (message.isRaining() && this.rainLabel.getOpacity() == 0.0) {
                FadeTransition fadeInTransition = new FadeTransition(Duration.seconds(2), this.rainLabel);
                fadeInTransition.setFromValue(0.0);
                fadeInTransition.setToValue(1.0);
                fadeInTransition.play();
            } else if (!message.isRaining() && this.rainLabel.getOpacity() == 1.0) {
                FadeTransition fadeOutTransition = new FadeTransition(Duration.seconds(2), this.rainLabel);
                fadeOutTransition.setFromValue(1.0);
                fadeOutTransition.setToValue(0.0);
                fadeOutTransition.play();
            }

            this.cycleTimeline.jumpTo(CYCLE_TIME.multiply((message.getWorldTime() / 24000.0d)));
        });
    }
}
