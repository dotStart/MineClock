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
package tv.dotstart.minecraft.clock.control;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.DefaultProperty;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javax.annotation.Nonnull;

/**
 * Provides a specialized control which replicates a standard title bar which allows users to drag a
 * window around, minimize and close it.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
@DefaultProperty("additionalButtons")
public class TitleBar extends HBox implements Initializable {

  private final ObservableList<Node> additionalButtons = FXCollections.observableArrayList();
  private final BooleanProperty closeable = new SimpleBooleanProperty(true);
  private final BooleanProperty closesApplication = new SimpleBooleanProperty(false);
  private final BooleanProperty minimizable = new SimpleBooleanProperty(true);
  private final StringProperty title = new SimpleStringProperty("Test");
  @FXML
  private HBox buttons = new HBox();
  @FXML
  private Button closeButton = new Button("\uf00d");
  // </editor-fold>
  @FXML
  private Button iconifyButton = new Button("\uf070");
  // <editor-fold desc="Window Dragging">
  private double initialX;
  private double initialY;
  // <editor-fold desc="FXML Elements">
  @FXML
  private Label titleLabel = new Label();
  // </editor-fold>

  public TitleBar() {
    FXMLLoader loader = new FXMLLoader(this.getClass().getResource("/fxml/control/TitleBar.fxml"));
    loader.setRoot(this);
    loader.setController(this);

    try {
      loader.load();
    } catch (IOException ex) {
      throw new RuntimeException("Could not load TitleBar: " + ex.getMessage(), ex);
    }
  }

  @Nonnull
  public BooleanProperty closeableProperty() {
    return closeable;
  }

  @Nonnull
  public BooleanProperty closesApplicationProperty() {
    return closesApplication;
  }

  @Nonnull
  public ObservableList<Node> getAdditionalButtons() {
    return this.additionalButtons;
  }

  // <editor-fold desc="Getters & Setters">
  public String getTitle() {
    return title.get();
  }

  public void setTitle(String title) {
    this.title.set(title);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialize(@Nonnull URL location, ResourceBundle resources) {
    this.titleLabel.textProperty().bind(this.title);

    this.iconifyButton.visibleProperty().bind(this.minimizableProperty());
    this.iconifyButton.managedProperty().bind(this.iconifyButton.visibleProperty());

    this.closeButton.visibleProperty().bind(this.closeableProperty());
    this.closeButton.managedProperty().bind(this.closeButton.visibleProperty());

    // Note: This is a dirty fix which works around FXMLLoader assuming that we mean to include
    // contents declared in TitleBar.fxml as part of our additional buttons
    this.getChildren().addAll(this.additionalButtons);
    this.additionalButtons.clear();

    Bindings.bindContent(this.buttons.getChildren(), this.additionalButtons);
  }

  public boolean isCloseable() {
    return closeable.get();
  }

  public void setCloseable(boolean closeable) {
    this.closeable.set(closeable);
  }

  public boolean isClosesApplication() {
    return closesApplication.get();
  }

  public void setClosesApplication(boolean closesApplication) {
    this.closesApplication.set(closesApplication);
  }

  public boolean isMinimizable() {
    return minimizable.get();
  }

  public void setMinimizable(boolean minimizable) {
    this.minimizable.set(minimizable);
  }

  @Nonnull
  public BooleanProperty minimizableProperty() {
    return minimizable;
  }

  @FXML
  private void onClose(@Nonnull ActionEvent event) {
    if (this.closesApplicationProperty().get()) {
      Platform.exit();
    } else {
      ((Stage) this.getScene().getWindow()).close();
    }
  }
  // </editor-fold>

  // <editor-fold desc="Event Handlers">
  @FXML
  private void onIconify(@Nonnull ActionEvent event) {
    ((Stage) this.getScene().getWindow()).setIconified(true);
  }

  @FXML
  private void onMouseDragged(@Nonnull MouseEvent event) {
    if (event.getButton() != MouseButton.MIDDLE) {
      this.getScene().getWindow().setX(event.getScreenX() - initialX);
      this.getScene().getWindow().setY(event.getScreenY() - initialY);
    }
  }

  @FXML
  private void onMousePressed(@Nonnull MouseEvent event) {
    if (event.getButton() != MouseButton.MIDDLE) {
      initialX = event.getSceneX();
      initialY = event.getSceneY();
    } else {
      this.getScene().getWindow().centerOnScreen();
      initialX = this.getScene().getWindow().getX();
      initialY = this.getScene().getWindow().getY();
    }
  }

  @Nonnull
  public StringProperty titleProperty() {
    return title;
  }
  // </editor-fold>
}
