package ru.geekbrains;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
public class EchoHandler extends SimpleChannelInboundHandler<String> {

    private static final ConcurrentLinkedDeque<ChannelHandlerContext> clients = new ConcurrentLinkedDeque<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String s) throws Exception {
        log.debug("received: {}", s);
        System.out.println("received: " + s);
        clients.forEach(context -> context.writeAndFlush(s));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Client connected...");
        System.out.println("Client connected...");
        clients.add(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Client disconnected...");
        System.out.println("Client disconnected...");
        clients.remove(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("", cause);
        System.out.println(cause);
    }
}
