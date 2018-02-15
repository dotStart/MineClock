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

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import tv.dotstart.minecraft.clock.MineClockApplication;
import tv.dotstart.minecraft.clock.service.ConfigurationService;

/**
 * Provides a handler for all functionality declared by {@code SettingsWindow.fxml}.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class SettingsWindowController implements Initializable {

  private final ConfigurationService configurationService;
  @FXML
  private CheckBox allowSynchronization;
  @FXML
  private CheckBox displayWeather;
  @FXML
  private CheckBox launchPortraitMode;
  @FXML
  private Label versionLabel;

  @Inject
  public SettingsWindowController(@Nonnull ConfigurationService configurationService) {
    this.configurationService = configurationService;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    // Settings Tab
    this.launchPortraitMode.selectedProperty()
        .bindBidirectional(this.configurationService.launchPortraitModeProperty());
    this.allowSynchronization.selectedProperty()
        .bindBidirectional(this.configurationService.allowSynchronizationProperty());
    this.displayWeather.selectedProperty()
        .bindBidirectional(this.configurationService.displayWeatherProperty());

    // About Tab
    {
      Package p = this.getClass().getPackage();
      String version = (p == null ? null : p.getImplementationVersion());

      this.versionLabel
          .setText(String.format(this.versionLabel.getText(), (version == null ? "?" : version)));
    }
  }

  @FXML
  private void onForums() {
    try {
      Desktop.getDesktop().browse(new URI(
          "http://www.minecraftforum.net/forums/mapping-and-modding/minecraft-tools/2746931"));
    } catch (URISyntaxException | IOException ex) {
      MineClockApplication.reportError(ex);
    }
  }

  @FXML
  private void onOpenSourceInformation() {
    try {
      Desktop.getDesktop()
          .browse(new URI("https://github.com/LordAkkarin/MineClock/wiki/ThirdPartyLicenses"));
    } catch (URISyntaxException | IOException ex) {
      MineClockApplication.reportError(ex);
    }
  }

  @FXML
  private void onSource() {
    try {
      Desktop.getDesktop().browse(new URI("https://github.com/LordAkkarin/MineClock"));
    } catch (URISyntaxException | IOException ex) {
      MineClockApplication.reportError(ex);
    }
  }
}
