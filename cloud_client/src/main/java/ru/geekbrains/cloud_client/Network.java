package ru.geekbrains.cloud_client;

public class Network<I, O> {

    public Network(I inputStream, O outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    private final I inputStream;

    private final O outputStream;

    public I getInputStream() {
        return inputStream;
    }

    public O getOutputStream() {
        return outputStream;
    }

}
