package ru.geekbrains.cloud_client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import ru.geekbrains.DaemonThreadFactory;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.*;

import static ru.geekbrains.Command.*;
import static ru.geekbrains.FileUtils.readFileFromStream;

public class CloudMainController implements Initializable {
    public ListView<String> clientView;

    public ListView<String> serverView;

    private String currentDirectory;

    private DataInputStream dis;

    private DataOutputStream dos;

    private Socket socket;

    private boolean needReadMessages = true;

    private DaemonThreadFactory factory;

    public void sendToServer(ActionEvent actionEvent) {
        String fileName = clientView.getSelectionModel().getSelectedItem();
        String filePath = currentDirectory + "/" + fileName;
        File file = new File(filePath);
        if (file.isFile()) {
            try {
                System.out.println("File: " + fileName + " sent to server");
                dos.writeUTF(SEND_FILE_COMMAND.getSimpleName());
                dos.writeUTF(fileName);
                dos.writeLong(file.length());
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] bytes = fis.readAllBytes();
                    dos.write(bytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } catch (Exception e) {
                System.err.println("e = " + e.getMessage());
            }
        }
    }

    private void readMessages() {
        try {
            while (needReadMessages) {
                String command = dis.readUTF();
                if (SEND_FILE_COMMAND.getSimpleName().equals(command)) {
                    readFileFromStream(dis, currentDirectory);
                    Platform.runLater(() -> fillView(clientView, getFiles(currentDirectory)));
                } else if (GET_FILES_LIST_COMMAND.getSimpleName().equals(command)) {
                    System.out.println("Received command: " + GET_FILES_LIST_COMMAND.getSimpleName());
                    List<String> files = new ArrayList<>();
                    int size = dis.readInt();
                    for (int i = 0; i < size; i++) {
                        String file = dis.readUTF();
                        files.add(file);
                    }
                    Platform.runLater(() -> fillView(serverView, files));
                }
            }
        } catch (Exception e) {
            System.err.println("Server off");
        }
    }

    private void initNetwork() {
        try {
            socket = new Socket("localhost", 8189);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
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
                File selectedFile = new File(currentDirectory + "/" + selected);
                if (selectedFile.isDirectory()) {
                    setCurrentDirectory(currentDirectory + "/" + selected);
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
        dos.writeUTF(GET_FILE_COMMAND.getSimpleName());
        dos.writeUTF(fileName);
        dos.flush();
    }
}
