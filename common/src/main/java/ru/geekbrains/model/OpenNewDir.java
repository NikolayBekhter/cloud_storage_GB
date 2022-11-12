package ru.geekbrains.model;

import lombok.Getter;

@Getter
public class OpenNewDir implements CloudMessage {

    private final String newDir;

    public OpenNewDir(String newDir) {
        this.newDir = newDir;
    }

    @Override
    public MessageType getType() {
        return MessageType.OPEN_NEW_DIR;
    }
}
