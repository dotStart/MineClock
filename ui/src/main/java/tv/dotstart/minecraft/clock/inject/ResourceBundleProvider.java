/*
 * Copyright 2018 Johannes Donath <johannesd@torchmind.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tv.dotstart.minecraft.clock.inject;

import java.util.Locale;
import java.util.Locale.Category;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import javax.annotation.Nonnull;
import javax.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provides localizations to the application (and falls back to the default locale if the system
 * locale is not actually supported).
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class ResourceBundleProvider implements Provider<ResourceBundle> {

  private static final Logger logger = LogManager.getFormatterLogger(ResourceBundleProvider.class);

  /**
   * Defines the locale to fall back to when the system locale is not supported by the application.
   */
  private static final Locale FALLBACK_LOCALE = Locale.ENGLISH;

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  public ResourceBundle get() {
    return this.get(Locale.getDefault(Category.DISPLAY))
        .orElseGet(() -> {
          logger.warn("Locale %s is not supported by the application - Falling back to English",
              Locale.getDefault(Category.DISPLAY));

          return this.get(FALLBACK_LOCALE)
              .orElseThrow(() -> new RuntimeException("Failed to load fallback localization"));
        });
  }

  /**
   * Retrieves a localization for a specific locale.
   */
  @Nonnull
  private Optional<ResourceBundle> get(@Nonnull Locale locale) {
    try {
      return Optional.of(ResourceBundle.getBundle("localization/messages", locale));
    } catch (MissingResourceException ex) {
      return Optional.empty();
    }
  }
}
