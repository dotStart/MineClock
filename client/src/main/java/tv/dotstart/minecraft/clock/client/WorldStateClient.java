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
package tv.dotstart.minecraft.clock.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.net.InetSocketAddress;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;
import tv.dotstart.minecraft.clock.network.WorldStatePacket;

/**
 * Provides a client implementation which is capable of pushing world state updates to a local
 * server.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class WorldStateClient {

  /**
   * Defines the standard hostname on which the server typically listens for incoming world state
   * updates.
   */
  public static final String DEFAULT_HOSTNAME = "127.0.0.1";

  /**
   * Defines the standard port on which the server typically listens for incoming world state
   * updates.
   */
  public static final int DEFAULT_PORT = 52262;

  private final InetSocketAddress address;

  private final Lock lock = new ReentrantLock();
  private EventLoopGroup eventLoopGroup;
  private Channel channel;

  public WorldStateClient(@Nonnull InetSocketAddress address) {
    this.address = address;
  }

  public WorldStateClient() {
    this(new InetSocketAddress(DEFAULT_HOSTNAME, DEFAULT_PORT));
  }

  /**
   * Establishes a "connection" with the specified MineClock instance.
   */
  public void connect() {
    this.lock.lock();

    try {
      if (this.channel != null) {
        return;
      }

      this.eventLoopGroup = new NioEventLoopGroup();

      ChannelFuture future = new Bootstrap()
          .channel(NioDatagramChannel.class)
          .group(this.eventLoopGroup)
          .handler(new ChannelInitializer<DatagramChannel>() {
            @Override
            protected void initChannel(DatagramChannel ch) throws Exception {
              // NOOP
            }
          })
          .connect(this.address)
          .awaitUninterruptibly();

      if (!future.isSuccess()) {
        Throwable cause = future.cause();

        if (cause == null) {
          cause = new IllegalStateException("Unknown Error");
        }

        this.eventLoopGroup.shutdownGracefully();
        throw new IllegalStateException("Failed to establish connection: " + cause.getMessage(),
            cause);
      }

      this.channel = future.channel();
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Disconnects from the MineClock server and shuts down all remaining threads.
   */
  public void disconnect() {
    this.lock.lock();

    try {
      if (this.channel == null) {
        return;
      }

      this.channel.close().awaitUninterruptibly();
      this.eventLoopGroup.shutdownGracefully().awaitUninterruptibly();

      this.channel = null;
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Prepares an update to the server.
   */
  @Nonnull
  public UpdateBuilder update() {
    return new UpdateBuilder();
  }

  /**
   * Provides a builder for world state updates.
   */
  public final class UpdateBuilder {

    private int worldTime;
    private boolean currentlyRaining;

    private UpdateBuilder() {
    }

    @Nonnull
    public WorldStatePacket buildPacket() {
      return new WorldStatePacket(this.worldTime, this.currentlyRaining);
    }

    /**
     * Assembles the binary representation of the update and sends it to the server.
     *
     * @throws IllegalStateException when the client is not connected at the moment.
     */
    public void push() {
      WorldStateClient.this.lock.lock();

      try {
        if (WorldStateClient.this.channel == null) {
          throw new IllegalStateException("Cannot push update: Client is not connected");
        }

        ByteBuf data = WorldStateClient.this.channel.alloc().buffer();
        this.buildPacket().write(data);

        WorldStateClient.this.channel.writeAndFlush(
            new DatagramPacket(data, WorldStateClient.this.address)
        );
      } finally {
        WorldStateClient.this.lock.unlock();
      }
    }

    @Nonnull
    public UpdateBuilder setWorldTime(int worldTime) {
      this.worldTime = worldTime;
      return this;
    }

    @Nonnull
    public UpdateBuilder setCurrentlyRaining(boolean currentlyRaining) {
      this.currentlyRaining = currentlyRaining;
      return this;
    }
  }
}
