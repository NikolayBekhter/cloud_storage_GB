package ru.geekbrains.serial;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import ru.geekbrains.model.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class FileHandler extends SimpleChannelInboundHandler<CloudMessage> {

    private Path serverDir;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        serverDir = Path.of("server_files");
        ctx.writeAndFlush(new ListMessage(serverDir));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudMessage cloudMessage) throws Exception {
        log.debug("Received: {}", cloudMessage.getType());
        System.out.println("Received: " + cloudMessage.getType());
        if (cloudMessage instanceof FileMessage fileMessage) {
            Files.write(serverDir.resolve(fileMessage.getFileName()), fileMessage.getBytes());
            ctx.writeAndFlush(new ListMessage(serverDir));
        } else if (cloudMessage instanceof FileRequest fileRequest) {
            ctx.writeAndFlush(new FileMessage(serverDir.resolve(fileRequest.getFileName())));
        } else if (cloudMessage instanceof OpenNewDir newDir) {
            File dir = new File(serverDir + "/" + newDir.getNewDir());
            if (dir.isDirectory()) {
                serverDir = dir.toPath();
                ctx.writeAndFlush(new ListMessage(serverDir));
            }
        } else if (cloudMessage instanceof UpServerDir) {
            if (serverDir.getParent() != null) {
                serverDir = serverDir.getParent();
                ctx.writeAndFlush(new ListMessage(serverDir));
            }
        }
    }
}
