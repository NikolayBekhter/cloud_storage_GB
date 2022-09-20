package ru.geekbrains;

import java.util.concurrent.ThreadFactory;

public class DaemonThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    }

    public Thread getThread(Runnable r, String name) {
        Thread thread = newThread(r);
        thread.setName(name);
        thread.setDaemon(true);
        return thread;
    }
}
