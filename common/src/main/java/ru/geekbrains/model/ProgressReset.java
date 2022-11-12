package ru.geekbrains.model;

public class ProgressReset implements CloudMessage{
    @Override
    public MessageType getType() {
        return MessageType.PROGRESS_RESET;
    }
}
