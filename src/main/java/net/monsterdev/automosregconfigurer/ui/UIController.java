package net.monsterdev.automosregconfigurer.ui;

import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import net.monsterdev.automosregconfigurer.AutoMosregConfigurer;

import java.net.URL;
import java.util.ResourceBundle;

public interface UIController extends Initializable {
    static Stage primaryStage() {
        return AutoMosregConfigurer.primaryStage;
    }

    static void showErrorMessage(String msg) {
        Alert errorDlg = new Alert(Alert.AlertType.ERROR);
        errorDlg.setTitle("AutoMosreg");
        errorDlg.setHeaderText("Ошибка:");
        errorDlg.setContentText(msg);
        errorDlg.showAndWait();
    }

    static void showInfoMessage(String msg) {
        Alert errorDlg = new Alert(Alert.AlertType.INFORMATION);
        errorDlg.setTitle("AutoMosreg");
        errorDlg.setHeaderText("Информация:");
        errorDlg.setContentText(msg);
        errorDlg.showAndWait();
    }

    void initialize(URL location, ResourceBundle resources);

    Node getView();
    void setView(Node node);
}
