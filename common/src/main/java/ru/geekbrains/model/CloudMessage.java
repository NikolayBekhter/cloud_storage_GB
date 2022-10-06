package ru.geekbrains.model;

import java.io.Serializable;

public interface CloudMessage extends Serializable {
    MessageType getType();
}
