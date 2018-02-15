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
package tv.dotstart.minecraft.clock.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javax.annotation.Nonnull;
import javax.inject.Singleton;
import tv.dotstart.minecraft.clock.MineClockApplication;

/**
 * Provides a service which is capable of manging configuration properties and changes to said
 * properties.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
@Singleton
public class ConfigurationService {

  private final BooleanProperty allowSynchronization = new SimpleBooleanProperty();
  private final BooleanProperty displayWeather = new SimpleBooleanProperty();
  private final BooleanProperty launchPortraitMode = new SimpleBooleanProperty();
  private final Properties properties = new Properties();

  public ConfigurationService() {
    this.loadConfiguration();

    // Hook Changes
    ChangeListener listener = new ConfigurationChangeListener();

    this.launchPortraitMode.addListener(listener);
    this.allowSynchronization.addListener(listener);
    this.displayWeather.addListener(listener);
  }

  @Nonnull
  private Path getConfigurationPath() {
    return MineClockApplication.getApplicationDirectory().resolve("application.conf");
  }

  // <editor-fold desc="Getters and Setters">
  public boolean isLaunchPortraitMode() {
    return this.launchPortraitMode.get();
  }

  public void setLaunchPortraitMode(boolean launchPortraitMode) {
    this.launchPortraitMode.set(launchPortraitMode);
  }

  @Nonnull
  public BooleanProperty launchPortraitModeProperty() {
    return this.launchPortraitMode;
  }

  public boolean isAllowSynchronization() {
    return this.allowSynchronization.get();
  }

  @Nonnull
  public BooleanProperty allowSynchronizationProperty() {
    return this.allowSynchronization;
  }

  public void setAllowSynchronization(boolean allowSynchronization) {
    this.allowSynchronization.set(allowSynchronization);
  }

  @Nonnull
  public BooleanProperty displayWeatherProperty() {
    return this.displayWeather;
  }

  public boolean isDisplayWeather() {
    return this.displayWeather.get();
  }

  public void setDisplayWeather(boolean displayWeather) {
    this.displayWeather.set(displayWeather);
  }
  // </editor-fold>

  private void loadConfiguration() {
    Path configurationFile = this.getConfigurationPath();

    if (Files.notExists(configurationFile)) {
      this.allowSynchronization.set(true);
      this.displayWeather.set(true);

      this.saveConfiguration();
      return;
    }

    try (InputStream inputStream = new FileInputStream(configurationFile.toFile())) {
      this.properties.loadFromXML(inputStream);
    } catch (IOException ex) {
      throw new RuntimeException(
          "Could not load application configuration file: " + ex.getMessage(), ex);
    }

    this.launchPortraitMode.set(Boolean.valueOf(this.properties.getProperty("launch-in-portrait")));
    this.allowSynchronization
        .set(Boolean.valueOf(this.properties.getProperty("allow-synchronization")));
    this.displayWeather.set(Boolean.valueOf(this.properties.getProperty("display-weather")));
  }

  private void saveConfiguration() {
    this.properties
        .setProperty("launch-in-portrait", Boolean.toString(this.isLaunchPortraitMode()));
    this.properties
        .setProperty("allow-synchronization", Boolean.toString(this.isAllowSynchronization()));
    this.properties.setProperty("display-weather", Boolean.toString(this.isDisplayWeather()));

    try (OutputStream outputStream = new FileOutputStream(this.getConfigurationPath().toFile())) {
      this.properties.storeToXML(outputStream, "MineClock Configuration File - DO NOT EDIT");
    } catch (IOException ex) {
      throw new RuntimeException("Could not store application configuration: " + ex.getMessage(),
          ex);
    }
  }

  /**
   * Provides a change listener which automatically updates the local version of the configuration
   * file in order to keep it in sync with user preferences represented within the application.
   */
  private class ConfigurationChangeListener implements ChangeListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public void changed(ObservableValue observable, Object oldValue, Object newValue) {
      ConfigurationService.this.saveConfiguration();
    }
  }
}
