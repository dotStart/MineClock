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
package tv.dotstart.minecraft.clock.inject;

import com.google.inject.Injector;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Provides a factory for instances of {@link FXMLLoader} which have been augmented with Guice to
 * properly implement JSR 330.
 *
 * <strong>Note:</strong> This is a workaround to be able to pass singletons between controllers
 * properly which would usually not be possible with JavaFX's injection capabilities.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class FXMLProvider implements Provider<FXMLLoader> {

  private final Injector injector;

  @Inject
  public FXMLProvider(@Nonnull Injector injector) {
    this.injector = injector;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FXMLLoader get() {
    FXMLLoader loader = new FXMLLoader();
    loader.setCharset(StandardCharsets.UTF_8);
    loader.setControllerFactory(this.injector::getInstance);
    loader.setResources(this.injector.getInstance(ResourceBundle.class));
    return loader;
  }
}
