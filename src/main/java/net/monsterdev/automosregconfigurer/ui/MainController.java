package net.monsterdev.automosregconfigurer.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.monsterdev.automosregconfigurer.exceptions.ConfigurerException;
import net.monsterdev.automosregconfigurer.model.CertificateInfo;
import net.monsterdev.automosregconfigurer.utils.CertificatesHelper;
import net.monsterdev.automosregconfigurer.utils.OSValidator;
import net.monsterdev.automosregconfigurer.utils.WinRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainController extends AbstractUIController implements WindowController {

  private static final Logger logger = LogManager.getLogger(MainController.class);

  private static final String JDK_PATH = "SOFTWARE\\JavaSoft\\Java Development Kit";
  private static final String JRE_PATH = "SOFTWARE\\JavaSoft\\Java Runtime Environment";

  @FXML
  private ComboBox<String> cmbInstalledJREPath;
  private ToggleGroup jreChoice = new ToggleGroup();
  @FXML
  private RadioButton optInstalledJRE;
  @FXML
  private RadioButton optBrowseJRE;
  @FXML
  private Pane installedJRE;
  @FXML
  private Pane browseJRE;
  @FXML
  private TextField edtBrowseJREPath;
  @FXML
  private TextField edtJCPPath;

  @FXML
  private TextField edtCertificateCN;

  private CertificateInfo certificateInfo;

  private String jrePath;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    Set<String> jreInstalledPaths = new HashSet<>();
    try {
      // список установленных JDK
      List<String> installedJavaVersions;
      // получаем список установленных JDK
      installedJavaVersions = WinRegistry.subKeysForPath(WinRegistry.HKEY_LOCAL_MACHINE, JDK_PATH);
      for (String jdk : installedJavaVersions) {
        jreInstalledPaths.add(WinRegistry
            .valueForKey(WinRegistry.HKEY_LOCAL_MACHINE, JDK_PATH + "\\" + jdk, "JavaHome") + "\\jre\\");
      }
      // получаем список установленных JRE
      installedJavaVersions = WinRegistry.subKeysForPath(WinRegistry.HKEY_LOCAL_MACHINE, JRE_PATH);
      for (String jre : installedJavaVersions) {
        jreInstalledPaths.add(WinRegistry
            .valueForKey(WinRegistry.HKEY_LOCAL_MACHINE, JRE_PATH + "\\" + jre, "JavaHome") + "\\");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    /*
    // Используя переменную среды JAVA_HOME определяем путь, по которому установлен JRE
    String jhPath = System.getenv("JAVA_HOME");
    if (jhPath != null) {
      if (!jhPath.endsWith(File.separator)) {
        jhPath = jhPath.concat(File.separator);
      }
      // Если в каталоге, на который указывает JAVA_HOME существует каталоге jre, то JAVA_HOME указывает на
      // JDK, а нам нужен соответствующий JRE
      if (Files.exists(Paths.get(jhPath + "jre"))) {
        jreInstalledPaths.add(jhPath + "jre\\");
      }
    }
    */
    cmbInstalledJREPath.getItems().addAll(jreInstalledPaths);
    cmbInstalledJREPath.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> jrePath = newValue);

    optInstalledJRE.setToggleGroup(jreChoice);
    optInstalledJRE.setSelected(true);

    optBrowseJRE.setToggleGroup(jreChoice);
    browseJRE.setDisable(true);

    jreChoice.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
      if (jreChoice.getSelectedToggle() == optInstalledJRE) {
        jrePath = cmbInstalledJREPath.getValue();
        installedJRE.setDisable(false);
        browseJRE.setDisable(true);
      } else if (jreChoice.getSelectedToggle() == optBrowseJRE) {
        jrePath = edtBrowseJREPath.getText();
        installedJRE.setDisable(true);
        browseJRE.setDisable(false);
      }
    });
  }

  @FXML
  private void onBrowseJRE() {
    DirectoryChooser dlg = new DirectoryChooser();
    dlg.setTitle("Выбор каталога JRE");
    File result = dlg.showDialog(getStage());
    if (result != null) {
      jrePath = result.getAbsolutePath();
      edtBrowseJREPath.setText(jrePath);
    }
  }

  @FXML
  private void onBrowseJCP() {
    FileChooser dlg = new FileChooser();
    dlg.setTitle("Выбор КриптоПро JCP");
    dlg.getExtensionFilters().addAll(//
        new FileChooser.ExtensionFilter("All Files", "*.*"),
        new FileChooser.ExtensionFilter("Zip File", "*.zip"));
    dlg.setInitialDirectory(new File(System.getProperty("user.home")));
    File result = dlg.showOpenDialog(getStage());
    if (result != null) {
      edtJCPPath.setText(result.getAbsolutePath());
    }
  }

  @FXML
  private void onSetupJCP() {
    if (jrePath == null || jrePath.isEmpty()) {
      UIController.showErrorMessage("Необходимо задать путь к JRE");
      return;
    }
    if (edtJCPPath.getText().isEmpty()) {
      UIController.showErrorMessage("Необходимо задать путь к дистрибутиву КриптоПро JCP");
      return;
    }
    // В корне архива JCP, скаченного с сайта КриптоПРО находится каталог вида jcp-2.0.40035
    // и все файлы дистрибутива лежат именно в нем. Имя этого каталога нам потребуется, чтобы установить JCP в систему.
    // Как его получить. Очень просто... Это будет имя самой первой entry, на которую мы наткнемся при распаковке архива
    File rootEntry = null;
    byte[] buffer = new byte[1024];
    File jcpDestDir;
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(edtJCPPath.getText()), Charset.forName("CP866"))) {
      // 1. Распаковка JCP во временный каталог
      jcpDestDir = Files.createTempDirectory("jcp").toFile();
      ZipEntry zipEntry = zis.getNextEntry();
      while (zipEntry != null) {
        File destFile = new File(jcpDestDir, zipEntry.getName());
        if (rootEntry == null) {
          rootEntry = destFile;
        }
        // Если текущий элемент архива есть каталог, то нужно создать соответствующий каталог
        if (zipEntry.isDirectory()) {
          if (!destFile.mkdirs()) {
            throw new IOException("Ошибка при распаковки дистрибутива КриптоПро JCP");
          }
        }
        // иначе это файл и его нужно распаковать
        else {
          FileOutputStream fos = new FileOutputStream(destFile);
          int readied;
          while ((readied = zis.read(buffer)) > 0) {
            fos.write(buffer, 0, readied);
          }
          fos.close();
        }
        zipEntry = zis.getNextEntry();
      }
      if (rootEntry == null) {
        throw new IOException("Указанный файл дистрибутива JCP поврежден");
      }
      // 2. Запуск установщика JCP
      String jcpRootPath = rootEntry.getAbsolutePath() + File.separator;
      String cmd;
      if (OSValidator.isWin()) {
        cmd = "setup.exe";
      } else if (OSValidator.isNix() || OSValidator.isMac()) {
        cmd = "setup_gui.sh";
      } else {
        throw new ConfigurerException("Неизвестная версия ОС");
      }
      cmd = jcpRootPath + cmd;

      Process jcpInstaller = new ProcessBuilder(cmd, jrePath)
          .directory(rootEntry)
          .start();
      jcpInstaller.waitFor();

      // 3. Настройка JCP после установки
      String dependenciesDir = jcpRootPath + "/dependencies/";
      String jreExtPath = jrePath + File.separator + "lib/ext/";
      // в каталоге dependenciesDir нужно найти файл bcpkix*.jar и файл bcprov*.jar и скопировать его в jre/lib/ext
      // но копировать нужно только в том случае, если их еще нет по пути назначению
      try (DirectoryStream<Path> dir = Files.newDirectoryStream(Paths.get(dependenciesDir), "*.jar")) {
        for (Path path : dir) {
          String fileName = path.getFileName().toString();
          if (fileName.matches("^(bcpkix|bcprov).*\\.jar$")) {
            Path destFilePath = Paths.get(jreExtPath + fileName);
            if (!Files.exists(destFilePath)) {
              Files.copy(path, destFilePath, StandardCopyOption.REPLACE_EXISTING);
            }
          }
        }
      } catch (Exception e) {
        UIController.showErrorMessage(
            String.format("При копировании необходимых библиотек произошла ошибка %s", e.getMessage()));
      }
    } catch (Exception ex) {
      UIController.showErrorMessage(String.format("При установке КриптоПро JCP произошла ошибка %s", ex.getMessage()));
    }
  }

  @FXML
  private void onSelectCertificate() {
    Optional<CertificateInfo> result = CertificateListController.showUI();
    result.ifPresent(certificate -> {
      certificateInfo = certificate;
      edtCertificateCN.setText(certificate.getName());
    });
  }

  @FXML
  private void onSetupCertificate() {
    try {
      if (jrePath == null || !Files.exists(Paths.get(jrePath))) {
        throw new ConfigurerException("Не задан путь к JRE или указанный путь не существует");
      }
      if (certificateInfo == null || certificateInfo.getCertificate() == null) {
        throw new ConfigurerException("Не задан сертификат пользователя");
      }
      CertificatesHelper certificatesHelper = new CertificatesHelper();
      String jtksPath = certificatesHelper.getJTKSPath(jrePath);
      //Устанавливаем все сертификаты головных центров сертификации
      logger.info("Setup root CAs certificates into {} keystore", jtksPath);
      logger.debug("Loading root CAs from resources");
      List<Certificate> rootCerts = certificatesHelper.loadRootCertificates();
      logger.debug("Root CAs loaded successfully");
      logger.debug("Adding root CAs into keystore");
      for (Certificate rootCert : rootCerts) {
        logger.debug("Adding root CA {}", rootCert);
        certificatesHelper.addCertificate(jtksPath, rootCert);
      }
      logger.debug("All root CAs added successfully");
      // получаем цепочку пользовательского сертификата
      logger.info("Getting certification path");
      CertPath userCertPath = certificatesHelper.getCertPath(rootCerts, certificateInfo.getCertificate());
      logger.debug("Certification path got successfully");
      //Устанавливаем все сертификаты из цепочки кроме пользовательского
      List<Certificate> intermediateCerts = userCertPath
          .getCertificates()
          .stream()
          .filter((Predicate<Certificate>) certificate -> !certificate.equals(certificateInfo.getCertificate()))
          .collect(Collectors.toList());
      logger.info("Setup intermediate certificates");
      for (Certificate intermediateCert : intermediateCerts) {
        logger.debug("Adding intermediate certificate {}", intermediateCert);
        certificatesHelper.addCertificate(jtksPath, intermediateCert);
      }
      UIController.showInfoMessage("Сертификат ЭП успешно установлен");
    } catch (Exception ex) {
      logger.error(String.format("Failed to setup certificate due error {}", ex));
      UIController
          .showErrorMessage("При установке сертификата произошла ошибка.\nСмотри журнал ошибок для подробностей");
    }
  }

  @Override
  public Stage getStage() {
    return UIController.primaryStage();
  }

  @Override
  public void close() {

  }
}
