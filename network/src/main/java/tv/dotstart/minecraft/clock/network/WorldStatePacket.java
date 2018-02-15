package tv.dotstart.minecraft.clock.network;

import io.netty.buffer.ByteBuf;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Provides a serializable representation of the current world state of a Minecraft server
 * (including its current weather and time).
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class WorldStatePacket {

  private final boolean raining;
  private final int time;

  public WorldStatePacket(int time, boolean raining) {
    this.time = time;
    this.raining = raining;
  }

  /**
   * Decodes a state packet from its binary representation.
   */
  public WorldStatePacket(@Nonnull ByteBuf buffer) {
    this.time = buffer.readUnsignedShort();
    this.raining = buffer.readBoolean();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }
    WorldStatePacket that = (WorldStatePacket) o;
    return this.time == that.time &&
        this.raining == that.raining;
  }

  public int getTime() {
    return this.time;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.time, this.raining);
  }

  public boolean isRaining() {
    return this.raining;
  }

  /**
   * Serializes the world state packet into the supplied buffer.
   */
  public void write(@Nonnull ByteBuf buffer) {
    buffer.writeShort(this.time);
    buffer.writeBoolean(this.raining);
  }
}
