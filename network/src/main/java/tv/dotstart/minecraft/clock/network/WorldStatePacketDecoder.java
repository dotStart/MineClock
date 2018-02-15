package tv.dotstart.minecraft.clock.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;

/**
 * Decodes incoming datagram packets into their respective POJO representation.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class WorldStatePacketDecoder extends MessageToMessageDecoder<DatagramPacket> {

  /**
   * {@inheritDoc}
   */
  @Override
  protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) {
    out.add(new WorldStatePacket(msg.content()));
  }
}
