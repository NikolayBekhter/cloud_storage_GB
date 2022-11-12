package ru.geekbrains.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileMessage implements CloudMessage{

    private final String fileName;
    private final long size;
    private final byte[] bytes;
    private boolean multipart;
    private boolean doProgress;

    @Override
    public MessageType getType() {
        return MessageType.FILE_MESSAGE;
    }

}
