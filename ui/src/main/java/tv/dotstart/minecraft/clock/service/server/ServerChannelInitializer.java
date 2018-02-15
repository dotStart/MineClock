package tv.dotstart.minecraft.clock.service.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.DatagramChannel;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import tv.dotstart.minecraft.clock.network.WorldStatePacketDecoder;

/**
 * Handles the initialization of server channels created by the world state synchronization server
 * implementation.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
@Singleton
public class ServerChannelInitializer extends ChannelInitializer<DatagramChannel> {

  private final Provider<WorldStateHandler> worldStateHandlerProvider;

  @Inject
  public ServerChannelInitializer(@Nonnull Provider<WorldStateHandler> worldStateHandlerProvider) {
    this.worldStateHandlerProvider = worldStateHandlerProvider;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void initChannel(@Nonnull DatagramChannel ch) throws Exception {
    ch.pipeline()
        .addLast(new WorldStatePacketDecoder())
        .addLast(this.worldStateHandlerProvider.get());
  }
}
