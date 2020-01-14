package net.monsterdev.automosregconfigurer.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import net.monsterdev.automosregconfigurer.AutoMosregConfigurer;
import net.monsterdev.automosregconfigurer.exceptions.ConfigurerException;
import net.monsterdev.automosregconfigurer.model.CertificateInfo;
import net.monsterdev.automosregconfigurer.utils.CertificatesHelper;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class CertificateListController extends AbstractUIController implements WindowController {
    private static final String UI = "/net/monsterdev/automosregconfigurer/ui/certificates.fxml";
    @FXML
    Pane rootPane;
    @FXML
    TableView<CertificateInfo> tblCertificates;
    // текущий выбранный сертификат
    private CertificateInfo certificateInfo = null;
    private CertificatesHelper certificatesHelper = new CertificatesHelper();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TableColumn<CertificateInfo, String> c1 = new TableColumn<>("Название");
        TableColumn<CertificateInfo, String> c2 = new TableColumn<>("Срок действия");
        c1.setCellValueFactory(new PropertyValueFactory<>("name"));
        c2.setCellValueFactory(new PropertyValueFactory<>("validity"));
        c1.prefWidthProperty().bind(tblCertificates.widthProperty().multiply(0.5));
        c2.prefWidthProperty().bind(tblCertificates.widthProperty().multiply(0.5));
        tblCertificates.getColumns().setAll(c1, c2);
        try {
            tblCertificates.getItems().setAll(certificatesHelper.getCertificates());
        } catch (ConfigurerException ex) {
            UIController.showErrorMessage(ex.getMessage());
        }
    }

    @Override
    public Stage getStage() {
        return (Stage) rootPane.getScene().getWindow();
    }

    private CertificateInfo getCertificateInfo() {
        return certificateInfo;
    }

    static Optional<CertificateInfo> showUI() {
        try {
            FXMLLoader loader = new FXMLLoader();
            Parent view = loader.load(AutoMosregConfigurer.getResourceAsStream(UI));
            CertificateListController controller = loader.getController();
            controller.setView(view);
            Stage stage = new Stage();
            stage.setScene(new Scene(view, 450, 355));
            stage.setTitle("AutoMosregConfigurer - Выбор сертификата");
            stage.showAndWait();
            return Optional.ofNullable(controller.getCertificateInfo());
        } catch (IOException e) {
            UIController.showErrorMessage("Ошибка открытия открытия окна выбора сетификата\n" +
                    "Переустановка приложения может решить проблему");
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @FXML
    private void onOk() {
        certificateInfo = tblCertificates.getSelectionModel().getSelectedItem();
        if (certificateInfo == null) {
            UIController.showErrorMessage("Необходимо выбрать сертификат");
            return;
        }
        getStage().fireEvent(new WindowEvent(getStage(), WindowEvent.WINDOW_CLOSE_REQUEST));
    }
}
