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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tv.dotstart.minecraft.clock.MineClockApplication;

/**
 * Provides a service which is capable of manging configuration properties and changes to said
 * properties.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
@Singleton
public class ConfigurationService {

  private static final Logger logger = LogManager.getFormatterLogger(ConfigurationService.class);

  private final BooleanProperty allowSynchronization = new SimpleBooleanProperty();
  private final BooleanProperty display24HourTime = new SimpleBooleanProperty();
  private final BooleanProperty displayWeather = new SimpleBooleanProperty();
  private final BooleanProperty launchPortraitMode = new SimpleBooleanProperty();
  private final Properties properties = new Properties();

  @SuppressWarnings("unchecked")
  public ConfigurationService() {
    this.loadConfiguration();

    // Hook Changes
    ChangeListener listener = new ConfigurationChangeListener();

    this.allowSynchronization.addListener(listener);
    this.display24HourTime.addListener(listener);
    this.launchPortraitMode.addListener(listener);
    this.displayWeather.addListener(listener);
  }

  @Nonnull
  private Path getConfigurationPath() {
    return MineClockApplication.getApplicationDirectory().resolve("application.conf");
  }

  // <editor-fold desc="Getters and Setters">
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

  public boolean isDisplay24HourTime() {
    return this.display24HourTime.get();
  }

  @Nonnull
  public BooleanProperty display24HourTimeProperty() {
    return this.display24HourTime;
  }

  public void setDisplay24HourTime(boolean display24HourTime) {
    this.display24HourTime.set(display24HourTime);
  }

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

    logger.info("Application configuration file: %s", configurationFile.toAbsolutePath());

    if (Files.notExists(configurationFile)) {
      logger.warn("No configuration file found - Falling back to defaults");

      this.allowSynchronization.set(true);
      this.display24HourTime.set(false);
      this.displayWeather.set(true);
      this.launchPortraitMode.set(false);

      this.saveConfiguration();
      return;
    }

    try (InputStream inputStream = new FileInputStream(configurationFile.toFile())) {
      this.properties.loadFromXML(inputStream);
    } catch (IOException ex) {
      throw new RuntimeException(
          "Could not load application configuration file: " + ex.getMessage(), ex);
    }

    this.allowSynchronization
        .set(Boolean.valueOf(this.properties.getProperty("allow-synchronization", "true")));
    this.display24HourTime
        .set(Boolean.valueOf(this.properties.getProperty("display-24h-time", "false")));
    this.launchPortraitMode
        .set(Boolean.valueOf(this.properties.getProperty("launch-in-portrait", "false")));
    this.displayWeather
        .set(Boolean.valueOf(this.properties.getProperty("display-weather", "true")));

    logger.info("Restored previous application configuration");
  }

  private void saveConfiguration() {
    logger.info("Writing configuration file to disk");

    this.properties.clear();
    this.properties
        .setProperty("allow-synchronization", Boolean.toString(this.isAllowSynchronization()));
    this.properties.setProperty("display-24h-time", Boolean.toString(this.isDisplay24HourTime()));
    this.properties
        .setProperty("launch-in-portrait", Boolean.toString(this.isLaunchPortraitMode()));
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
