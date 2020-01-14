package net.monsterdev.automosregconfigurer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.InputStream;
import java.net.URL;

public class AutoMosregConfigurer extends Application {
    public static final String APPLICATION_NAME = "AutoMosreg(Панель администратора)";

    public static Stage primaryStage = null;
    private static AutoMosregConfigurer instance = null;

    @Override
    public void start(Stage primaryStage) throws Exception {
        AutoMosregConfigurer.primaryStage = primaryStage;
        AutoMosregConfigurer.instance = this;
        Parent node = FXMLLoader.load(getResource("/net/monsterdev/automosregconfigurer/ui/main.fxml"));
        AutoMosregConfigurer.primaryStage.setTitle(APPLICATION_NAME + " - Главная");
        AutoMosregConfigurer.primaryStage.setScene(new Scene(node, 600, 400));
        AutoMosregConfigurer.primaryStage.show();
        AutoMosregConfigurer.primaryStage.setOnCloseRequest(event -> {
        });

    }

    public static URL getResource(String name) {
        return instance.getClass().getResource(name);
    }

    public static InputStream getResourceAsStream(String name) {
        return instance.getClass().getResourceAsStream(name);
    }

    public static void main(String[] args) {
        System.setProperty("com.sun.security.enableCRLDP", "true"); // для проверки по CRL DP
        System.setProperty("com.sun.security.enableAIAcaIssuers", "true"); // для загрузки сертификатов по AIA из сети
        System.setProperty("ru.CryptoPro.reprov.enableAIAcaIssuers", "true"); // для загрузки сертификатов по AIA из сети
        launch(args);
    }
}
