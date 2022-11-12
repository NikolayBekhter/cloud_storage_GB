package ru.geekbrains;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BufToString extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        log.debug("received: {}", buf);
        System.out.println(buf);
        StringBuilder s = new StringBuilder();
        while (buf.isReadable()) {
            s.append((char) buf.readByte());
        }
        ctx.fireChannelRead(s.toString());
    }
}
