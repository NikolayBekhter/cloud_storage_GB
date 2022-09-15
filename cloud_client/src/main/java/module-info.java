module ru.geekbrains.cloud_client {
    requires javafx.controls;
    requires javafx.fxml;


    opens ru.geekbrains.cloud_client to javafx.fxml;
    exports ru.geekbrains.cloud_client;
}