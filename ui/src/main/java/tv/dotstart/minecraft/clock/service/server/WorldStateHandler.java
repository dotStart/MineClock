package tv.dotstart.minecraft.clock.service.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javafx.application.Platform;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tv.dotstart.minecraft.clock.controller.MainWindowController;
import tv.dotstart.minecraft.clock.network.WorldStatePacket;

/**
 * Handles incoming world state updates from local clients.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class WorldStateHandler extends ChannelInboundHandlerAdapter {

  /**
   * Defines the total amount of ticks which have to pass for a full day/night cycle to be
   * completed.
   */
  public static final int MINECRAFT_DAY_LENGTH = 24000;
  private static final Logger logger = LogManager.getFormatterLogger(WorldStateHandler.class);
  private final MainWindowController controller;

  @Inject
  public WorldStateHandler(@Nonnull MainWindowController controller) {
    this.controller = controller;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void channelRead(@Nonnull ChannelHandlerContext ctx, @Nonnull Object msg)
      throws Exception {
    if (msg instanceof WorldStatePacket) {
      logger.info("Processing incoming world state update");
      WorldStatePacket packet = (WorldStatePacket) msg;

      Platform.runLater(() -> {
        logger.info("Updating state: World is at %d ticks (rain: %s)", packet.getTime(),
            packet.isRaining() ? "on" : "off");

        this.controller.setCycleTime(packet.getTime() / (double) MINECRAFT_DAY_LENGTH);
        // TODO: Handle rain
        this.controller.refreshSynchronization();
      });
    }

    super.channelRead(ctx, msg);
  }
}
