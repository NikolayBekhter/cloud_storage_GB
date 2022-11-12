package ru.geekbrains.serial;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import ru.geekbrains.constant.Constants;
import ru.geekbrains.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class FileHandler extends SimpleChannelInboundHandler<CloudMessage> {

    private Path serverDir;

    final Lock lock = new ReentrantLock();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        serverDir = Path.of("server_files");
        ctx.writeAndFlush(new ListMessage(serverDir));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudMessage cloudMessage) throws Exception {
        log.debug("Received: {}", cloudMessage.getType());
        if (cloudMessage instanceof FileMessage fileMessage) {
            Files.write(serverDir.resolve(fileMessage.getFileName()), fileMessage.getBytes());
            ctx.writeAndFlush(new ListMessage(serverDir));
        } else if (cloudMessage instanceof FileRequest fileRequest) {
            fileRequestResponse(ctx, fileRequest);
        } else if (cloudMessage instanceof OpenNewDir openNewDir) {
            String selected = openNewDir.getNewDir();
            Path path = Paths.get(String.valueOf(serverDir),selected).normalize();
            if(path.toFile().isDirectory()){
                if(!path.equals(serverDir)){
                    serverDir = path;
                    ctx.writeAndFlush(new ListMessage(serverDir));
                }
            }
        } else if (cloudMessage instanceof DeleteRequest deleteRequest) {
            Path deletedPath = Paths.get(String.valueOf((serverDir)),deleteRequest.getFileToDeleteName());
            try {
                if(deletedPath.toFile().isFile()){
                    Files.delete(deletedPath);
                    ctx.writeAndFlush(new ListMessage(serverDir));
                }else if(deletedPath.toFile().isDirectory()){
                    //рекурсивное удаление папок и файлов
                    deleteFolder(deletedPath);
                    ctx.writeAndFlush(new ListMessage(serverDir));
                }

            } catch (IOException e) {
                log.error("Попытка удаления не пустой директории!", e);
            }
        }
    }

    private void fileRequestResponse(ChannelHandlerContext ctx, FileRequest fileRequest) throws IOException {
        File file = serverDir.resolve(fileRequest.getFileName()).toFile();
        if(file.exists() && file.isFile()){
            long size = Files.size(serverDir.resolve(fileRequest.getFileName()));
            //если размер не превышает FILE_PACK_SIZE
            if (size <= Constants.FILE_PACK_SIZE) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    FileMessage fm = FileMessage.builder()
                            .multipart(false)
                            .fileName(fileRequest.getFileName())
                            .bytes(fis.readAllBytes())
                            .size(size)
                            .doProgress(false)
                            .build();
                    System.out.println(fm.getFileName()+" "+fm.getSize()+" -byte[]size_ simple send");
                    ctx.writeAndFlush(fm);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else {
                //если размер превышает FILE_PACK_SIZE
                Thread thread = new Thread(() -> {
                    lock.lock();
                    try(FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[Constants.FILE_PACK_SIZE];
                        long packages = (long) Math.ceil((double) size / Constants.FILE_PACK_SIZE);

                        long part = (long) (Math.ceil(packages)/100);
                        long count = 0;

                        int readBytes;
                        while ((readBytes = fis.read(buffer)) != -1){
                            if(count == part){
                                count = 0;
                                FileMessage fileMessage = FileMessage.builder()
                                        .multipart(true)
                                        .fileName(fileRequest.getFileName())
                                        .bytes(Arrays.copyOf(buffer, readBytes))
                                        .size(readBytes)
                                        .doProgress(true)
                                        .build();
                                ctx.writeAndFlush(fileMessage);

                            }else {
                                FileMessage fileMessage = FileMessage.builder()
                                        .multipart(true)
                                        .fileName(fileRequest.getFileName())
                                        .bytes(Arrays.copyOf(buffer, readBytes))
                                        .size(readBytes)
                                        .doProgress(false)
                                        .build();
                                ctx.writeAndFlush(fileMessage);
                            }
                            count++;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        log.debug("fileRequestResponse error");
                    }finally {
                        ctx.writeAndFlush(new ProgressReset());
                        lock.unlock();
                    }
                });
                thread.setDaemon(true);
                thread.start();
            }
        }
        if(file.isDirectory()){
            //Функционал не реализован
        }
    }

    private void deleteFolder(Path path) {
        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
