package ru.geekbrains.model;

import lombok.Getter;

@Getter
public class DeleteRequest implements CloudMessage{

    private final String fileToDeleteName;

    public DeleteRequest(String fileToDeleteName) {
        this.fileToDeleteName = fileToDeleteName;
    }

    @Override
    public MessageType getType() {
        return MessageType.DELETE_REQUEST;
    }
}
