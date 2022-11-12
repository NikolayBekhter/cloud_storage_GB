package ru.geekbrains.model;

public class UpServerDir implements CloudMessage{
    @Override
    public MessageType getType() {
        return MessageType.UP_SERVER_DIR;
    }
}
