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
package rocks.spud.minecraft.mineclock.attach.agent.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

/**
 * Provides a protocol implementation which allows inter-process communication with the
 * instrumentation host without introducing latencies or the need of special files.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class ClockProtocol {

  private final Bootstrap bootstrap;
  private final NioEventLoopGroup workerGroup = new NioEventLoopGroup();
  private final ObjectWriter writer;
  private Channel channel;
  private boolean server;

  public ClockProtocol() {
    this.writer = new ObjectMapper().writerFor(ClockMessage.class);

    this.bootstrap = new Bootstrap()
        .group(this.workerGroup)
        .channel(NioDatagramChannel.class)
        .handler(new SimpleChannelInboundHandler<DatagramPacket>() {
          @Override
          protected void messageReceived(ChannelHandlerContext ctx, DatagramPacket msg)
              throws Exception {
          }
        });
  }

  public ClockProtocol(@Nonnull Consumer<ClockMessage> consumer) {
    this();

    this.server = true;
    this.bootstrap.handler(new ClientHandler(consumer));
  }

  /**
   * Starts listening on localhost in order to communicate with the client or server.
   */
  public void listen() {
    try {
      this.channel = this.bootstrap.bind(InetAddress.getLocalHost(), (this.server ? 22565 : 0))
          .await().channel();
    } catch (UnknownHostException ex) {
      throw new IllegalStateException("Cannot listen on localhost: " + ex.getMessage(), ex);
    } catch (InterruptedException ex) {
      throw new IllegalStateException("Interrupted during channel creation: " + ex.getMessage(),
          ex);
    }
  }

  /**
   * Requests a new set of data from the server.
   */
  public void push(@Nonnull ClockMessage message) {
    try {
      this.channel.writeAndFlush(new DatagramPacket(
          this.channel.alloc().buffer().writeBytes(this.writer.writeValueAsBytes(message)),
          new InetSocketAddress(InetAddress.getLocalHost(), 22565)));
    } catch (JsonProcessingException | UnknownHostException ex) {
      throw new IllegalStateException(
          "Cannot send message from localhost to localhost: " + ex.getMessage(), ex);
    }
  }

  /**
   * Provides a simple client handler to handle incoming answers.
   */
  private class ClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final Consumer<ClockMessage> consumer;
    private final ObjectReader reader;

    public ClientHandler(Consumer<ClockMessage> consumer) {
      this.consumer = consumer;
      this.reader = new ObjectMapper().readerFor(ClockMessage.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void messageReceived(@Nonnull ChannelHandlerContext ctx, @Nonnull DatagramPacket msg)
        throws Exception {
      this.consumer.accept(this.reader.readValue(msg.content().toString(StandardCharsets.UTF_8)));
    }
  }
}
