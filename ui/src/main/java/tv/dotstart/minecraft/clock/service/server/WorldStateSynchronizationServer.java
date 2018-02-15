package tv.dotstart.minecraft.clock.service.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tv.dotstart.minecraft.clock.service.ConfigurationService;

/**
 * <p>Provides a synchronization server which accepts UDP messages from a compatible client in order
 * to synchronize the local time and weather.</p>
 *
 * <p>TODO: Add support for UNIX domain sockets on *NIX machines.</p>
 *
 * <p>TODO: Allow users to configure the server's port number</p>
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
@Singleton
public class WorldStateSynchronizationServer {

  /**
   * Defines the standard port on which the server typically listens for incoming world state
   * updates.
   */
  public static final int DEFAULT_PORT = 52262;
  private static final Logger logger = LogManager
      .getFormatterLogger(WorldStateSynchronizationServer.class);
  private final ConfigurationService configurationService;
  private final Lock lock = new ReentrantLock();
  private final ServerChannelInitializer serverChannelInitializer;
  private Channel channel;
  private NioEventLoopGroup eventLoopGroup;

  @Inject
  public WorldStateSynchronizationServer(
      @Nonnull ConfigurationService configurationService,
      @Nonnull ServerChannelInitializer serverChannelInitializer) {
    this.configurationService = configurationService;
    this.serverChannelInitializer = serverChannelInitializer;
  }

  /**
   * Evaluates whether or not to start the server upon startup.
   */
  public void postStartup() {
    this.configurationService.allowSynchronizationProperty().addListener(
        (observable, oldValue, newValue) -> {
          if (newValue) {
            this.start();
          } else {
            this.stop();
          }
        });

    if (this.configurationService.isAllowSynchronization()) {
      this.start();
    }
  }

  /**
   * Starts listening for new packets on the standard port.
   */
  public void start() {
    this.lock.lock();

    try {
      if (this.channel != null) {
        return;
      }

      logger.info("Initializing World State server ...");
      this.eventLoopGroup = new NioEventLoopGroup();

      Bootstrap bootstrap = new Bootstrap()
          .group(this.eventLoopGroup)
          .channel(NioDatagramChannel.class)
          .handler(this.serverChannelInitializer);

      logger.info("Binding to 127.0.0.1:%d", DEFAULT_PORT);
      ChannelFuture future = bootstrap.bind("127.0.0.1", DEFAULT_PORT).awaitUninterruptibly();

      if (!future.isSuccess()) {
        Throwable cause = future.cause();

        if (cause == null) {
          cause = new IllegalStateException("Unknown Error");
        }

        logger.error("Failed to start World State server: " + cause.getMessage(), cause);
        this.eventLoopGroup.shutdownGracefully().awaitUninterruptibly();
      }

      logger.info("Successfully started World State server");
      this.channel = future.channel();
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Stops listening for new packets on the standard port.
   */
  public void stop() {
    this.lock.lock();

    try {
      if (this.channel == null) {
        return;
      }

      logger.info("Shutting down World State server ...");
      this.channel.close().awaitUninterruptibly();
      this.eventLoopGroup.shutdownGracefully().awaitUninterruptibly();

      this.channel = null;
      logger.info("Server has been shut down");
    } finally {
      this.lock.unlock();
    }
  }
}
