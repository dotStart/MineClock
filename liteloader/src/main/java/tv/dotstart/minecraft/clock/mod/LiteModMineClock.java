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
package tv.dotstart.minecraft.clock.mod;

import com.mumfrey.liteloader.Tickable;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import javax.annotation.Nonnull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import tv.dotstart.minecraft.clock.client.WorldStateClient;

/**
 * Provides a mod implementation which automatically synchronizes the current game state to any
 * local MineClock instances.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class LiteModMineClock implements Tickable {

  private static final Duration SYNCHRONIZATION_PERIOD = Duration.ofSeconds(10);

  private final WorldStateClient client;

  private Instant lastSynchronization = Instant.EPOCH;

  public LiteModMineClock() {
    this.client = new WorldStateClient(); // TODO: Configuration
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onTick(@Nonnull Minecraft minecraft, float partialTicks, boolean inGame,
      boolean clock) {
    if (!inGame) {
      return;
    }

    if (this.lastSynchronization.plus(SYNCHRONIZATION_PERIOD).isAfter(Instant.now())) {
      return;
    }

    WorldClient world = minecraft.world;

    this.client.update()
        .setWorldTime((int) world.getWorldTime())
        .setGamePaused(minecraft.isGamePaused())
        .setCurrentlyRaining(world.isRaining())
        .push();

    this.lastSynchronization = Instant.now();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getVersion() {
    Package p = this.getClass().getPackage();
    return Optional.ofNullable(p.getImplementationVersion())
        .orElse("0.0.0");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void init(@Nonnull File configPath) {
    this.client.connect();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void upgradeSettings(@Nonnull String version, @Nonnull File configPath,
      @Nonnull File oldConfigPath) {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "MineClock";
  }
}
