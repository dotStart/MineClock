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
package rocks.spud.minecraft.mineclock.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javax.inject.Singleton;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import rocks.spud.minecraft.mineclock.MineClockApplication;

/**
 * Provides a service which is capable of manging configuration properties and changes to said
 * properties.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
@Singleton
public class ConfigurationService {
    private final BooleanProperty launchPortraitMode = new SimpleBooleanProperty();
    private final BooleanProperty automaticallyAttach = new SimpleBooleanProperty();
    private final BooleanProperty displayWeather = new SimpleBooleanProperty();
    private final Properties properties = new Properties();

    public ConfigurationService() {
        this.loadConfiguration();

        // Hook Changes
        ConfigurationChangeListener listener = new ConfigurationChangeListener();

        this.launchPortraitMode.addListener(listener);
        this.automaticallyAttach.addListener(listener);
        this.displayWeather.addListener(listener);
    }

    private Path getConfigurationPath() {
        return MineClockApplication.getApplicationDirectory().resolve("application.conf");
    }

    private void loadConfiguration() {
        Path configurationFile = this.getConfigurationPath();

        if (Files.notExists(configurationFile)) {
            this.automaticallyAttach.set(true);
            this.displayWeather.set(true);

            this.saveConfiguration();
            return;
        }

        try (InputStream inputStream = new FileInputStream(configurationFile.toFile())) {
            this.properties.loadFromXML(inputStream);
        } catch (IOException ex) {
            throw new RuntimeException("Could not load application configuration file: " + ex.getMessage(), ex);
        }

        this.launchPortraitMode.set(Boolean.valueOf(this.properties.getProperty("launch-in-portrait")));
        this.automaticallyAttach.set(Boolean.valueOf(this.properties.getProperty("automatically-attach")));
        this.displayWeather.set(Boolean.valueOf(this.properties.getProperty("display-weather")));
    }

    private void saveConfiguration() {
        this.properties.setProperty("launch-in-portrait", Boolean.toString(this.launchPortraitMode.get()));
        this.properties.setProperty("automatically-attach", Boolean.toString(this.automaticallyAttachProperty().get()));
        this.properties.setProperty("display-weather", Boolean.toString(this.displayWeather.get()));

        try (OutputStream outputStream = new FileOutputStream(this.getConfigurationPath().toFile())) {
            this.properties.storeToXML(outputStream, "MineClock Configuration File - DO NOT EDIT");
        } catch (IOException ex) {
            throw new RuntimeException("Could not store application configuration: " + ex.getMessage(), ex);
        }
    }

    // <editor-fold desc="Getters and Setters">
    public boolean isLaunchPortraitMode() {
        return launchPortraitMode.get();
    }

    public void setLaunchPortraitMode(boolean launchPortraitMode) {
        this.launchPortraitMode.set(launchPortraitMode);
    }

    public BooleanProperty launchPortraitModeProperty() {
        return launchPortraitMode;
    }

    public boolean isAutomaticallyAttach() {
        return automaticallyAttach.get();
    }

    public void setAutomaticallyAttach(boolean automaticallyAttach) {
        this.automaticallyAttach.set(automaticallyAttach);
    }

    public BooleanProperty automaticallyAttachProperty() {
        return automaticallyAttach;
    }

    public boolean isDisplayWeather() {
        return displayWeather.get();
    }

    public void setDisplayWeather(boolean displayWeather) {
        this.displayWeather.set(displayWeather);
    }

    public BooleanProperty displayWeatherProperty() {
        return displayWeather;
    }
    // </editor-fold>

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
            saveConfiguration();
        }
    }
}
