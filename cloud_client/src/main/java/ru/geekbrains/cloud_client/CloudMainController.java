package ru.geekbrains.cloud_client;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import ru.geekbrains.DaemonThreadFactory;
import ru.geekbrains.constant.Constants;
import ru.geekbrains.model.*;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CloudMainController implements Initializable {
    public ListView<String> clientView;

    public ListView<String> serverView;

    private String currentDirectory;

    private Network<ObjectDecoderInputStream, ObjectEncoderOutputStream> network;

    private Socket socket;

    private boolean needReadMessages = true;

    private DaemonThreadFactory factory;

    final Lock lock = new ReentrantLock();

    public ProgressBar progressBar;

    public void sendToServer(ActionEvent actionEvent) throws IOException {
        String fileName = clientView.getSelectionModel().getSelectedItem();
        long size = Files.size((Path.of(currentDirectory,fileName)));
        File file = Paths.get(currentDirectory,fileName).toFile();

        if(file.exists() && file.isFile()){
            writeFileToServer(fileName,size,file);
        }
        if(file.isDirectory()){
            writeDirectoryToServer(fileName);
        }
    }

    private void readMessages() {
        try {
            while (needReadMessages) {
                CloudMessage message = (CloudMessage) network.getInputStream().readObject();
                if (message instanceof FileMessage fileMessage) {
                    Files.write(Path.of(currentDirectory).resolve(fileMessage.getFileName()), fileMessage.getBytes());
                    Platform.runLater(() -> fillView(clientView, getFiles(currentDirectory)));
                } else if (message instanceof ListMessage listMessage) {
                    Platform.runLater(() -> fillView(serverView, listMessage.getFiles()));
                }
            }
        } catch (Exception e) {
            System.err.println("Server off");
            e.printStackTrace();
        }
    }

    private void initNetwork() {
        try {
            socket = new Socket("localhost", 8181);
            network = new Network<>(
                    new ObjectDecoderInputStream(socket.getInputStream()),
                    new ObjectEncoderOutputStream(socket.getOutputStream())
            );
            factory.getThread(this::readMessages, "cloud-client-read-thread")
                    .start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        needReadMessages = true;
        factory = new DaemonThreadFactory();
        initNetwork();
        setCurrentDirectory(System.getProperty("user.home"));
        fillView(clientView, getFiles(currentDirectory));
        clientView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = clientView.getSelectionModel().getSelectedItem();
                File selectedFile = new File(currentDirectory + "/" + selected);
                if (selectedFile.isDirectory()) {
                    setCurrentDirectory(currentDirectory + "/" + selected);
                }
            }
        });

        serverView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = serverView.getSelectionModel().getSelectedItem();
                try {
                    network.getOutputStream().writeObject(new OpenNewDir(selected));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

    }

    private void setCurrentDirectory(String directory) {
        currentDirectory = directory;
        fillView(clientView,  getFiles(currentDirectory));
    }

    private void fillView(ListView<String> view, List<String> data) {
        view.getItems().clear();
        view.getItems().addAll(data);
    }

    private List<String> getFiles(String directory) {
        File dir = new File(directory);
        if (dir.isDirectory()) {
            String[] list = dir.list();
            if (list != null) {
                List<String> files = new ArrayList<>(Arrays.asList(list));
                files.add(0, "..");
                return files;
            }
        }
        return List.of();
    }

    public void downLoadFile(ActionEvent actionEvent) throws IOException {
        String fileName = serverView.getSelectionModel().getSelectedItem();
        network.getOutputStream().writeObject(new FileRequest(fileName));
    }
    private void writeFileToServer(String fileName, long size, File file){

        //если размер не превышает FILE_PACK_SIZE
        if (size <= Constants.FILE_PACK_SIZE){
            try (FileInputStream fis = new FileInputStream(file)){
                FileMessage fm = FileMessage.builder()
                        .multipart(false)
                        .fileName(fileName)
                        .bytes(fis.readAllBytes())
                        .size(size)
                        .build();
                network.getOutputStream().writeObject(fm);
            }catch (IOException  e){
                e.printStackTrace();
            }
        }else{
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
                        FileMessage fileMessage = FileMessage.builder()
                                .multipart(true)
                                .fileName(fileName)
                                .bytes(Arrays.copyOf(buffer, readBytes))
                                .size(readBytes)
                                .build();
                        network.getOutputStream().writeObject(fileMessage);

                        count++;
                        if(count == part){
                            count = 0;
                            makeProgress();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    progressBar.setProgress(0);
                    lock.unlock();
                }
            });
            thread.setDaemon(true);
            thread.start();
        }
    }

    private void makeProgress() {
        progress(progressBar);
    }
    private void progress(ProgressBar p){
        //1%
        double value = p.getProgress();
        if(value < 0){
            value = 0.01;
        }else{
            value = value + 0.01;
            if(value >= 1.0){
                value = 1.0;
            }
        }
        p.setProgress(value);
    }

    private void writeDirectoryToServer(String dirName) {
        System.out.println("Отправка директории не реализована");
    }

    public void deleteFile(ActionEvent actionEvent) {
        String fileNameServer = serverView.getSelectionModel().getSelectedItem();
        String fileNameClient = clientView.getSelectionModel().getSelectedItem();
        if (fileNameServer != null) {
            try {
                network.getOutputStream().writeObject(new DeleteRequest(fileNameServer));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (fileNameClient != null) {
            try {
                Path filePath = Path.of(String.valueOf(Paths.get(currentDirectory, fileNameClient))).normalize();
                Files.delete(filePath);
                fillView(clientView, getFiles(currentDirectory));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
