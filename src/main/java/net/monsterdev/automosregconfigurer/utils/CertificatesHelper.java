package net.monsterdev.automosregconfigurer.utils;

import com.ibm.icu.text.Transliterator;
import lombok.NonNull;
import net.monsterdev.automosregconfigurer.AutoMosregConfigurer;
import net.monsterdev.automosregconfigurer.exceptions.ConfigurerException;
import net.monsterdev.automosregconfigurer.model.CertificateInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.security.cert.*;
import java.util.*;
import java.util.regex.Matcher;

public class CertificatesHelper {

  private static Logger logger = LogManager.getLogger(CertificatesHelper.class);

  /**
   * Возвращает список установленных сертификатов ЭЦП
   *
   * @return список сертификатов
   */
  public List<CertificateInfo> getCertificates() throws ConfigurerException {
    // Для получения списка сертификатов будем использовать провайдер JCP
    try {
      Provider provider = Security.getProvider("JCP");
      List<CertificateInfo> certificateInfos = new ArrayList<>();
      for (Provider.Service service : provider.getServices()) {
        if (service.getType().equals("KeyStore")) {
          String algorithm = service.getAlgorithm();
          logger.info("KeyStore service algorithm: {}", algorithm);
          KeyStore keyStore = KeyStore.getInstance(algorithm);
          logger.debug("Try to load keystore....");
          keyStore.load(null, null);
          logger.debug("Keystore loaded successfully");
          Enumeration<String> aliases = keyStore.aliases();
          while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            logger.debug("Processing alias: {}", alias);
            if (keyStore.isKeyEntry(alias)) {
              Certificate cert = keyStore.getCertificate(alias);
              if (cert != null && cert.getType().equals("X.509")) {
                X509Certificate x509Certificate = (X509Certificate) cert;
                X500Name x500Name = new JcaX509CertificateHolder(x509Certificate).getSubject();
                RDN cn = x500Name.getRDNs(BCStyle.CN)[0];
                CertificateInfo certificateInfo = new CertificateInfo();
                certificateInfo.setName(IETFUtils.valueToString(cn.getFirst().getValue()));
                certificateInfo.setValidFrom(x509Certificate.getNotBefore());
                certificateInfo.setValidTo(x509Certificate.getNotAfter());
                certificateInfo.setCertificate(cert);
                certificateInfos.add(certificateInfo);
              }
            }
          }
        }
      }
      return certificateInfos;
    } catch (Exception ex) {
      logger.error("", ex);
      throw new ConfigurerException(ex.getClass(), ex.getMessage());
    }
  }

  private Path getRootCAPath() throws URISyntaxException {
    URI rootCAURI = AutoMosregConfigurer.getResource("/rootCA").toURI();
    try {
      return Paths.get(rootCAURI);
    } catch (FileSystemNotFoundException ex) {
      try {
        FileSystem fs = FileSystems.newFileSystem(rootCAURI, Collections.emptyMap());
        return fs.provider().getPath(rootCAURI);
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
  }

  /**
   * Загружает и возвращает список корневых сертификатов из хранилища ресурсов приложения
   *
   * @return список корневых сертификатов их хранилища ресурсов приложения
   */
  public List<Certificate> loadRootCertificates() {
    List<Certificate> certs = new ArrayList<>();
    try (DirectoryStream<Path> dir = Files.newDirectoryStream(getRootCAPath(), "*.cer")) {
      CertificateFactory cf = CertificateFactory.getInstance("X509");
      for (Path entry : dir) {
        String entryName = "/rootCA/" + entry.getFileName().toString();
        certs.add(cf.generateCertificate(AutoMosregConfigurer.getResourceAsStream(entryName)));
      }
    } catch (IOException | CertificateException ex) {
      logger.error("Error while list root certificates: " + ex.getMessage());
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    return certs;
  }

  /**
   * Возвращает цепочку сертификатов для заданного сертификата
   *
   * @param rootCerts список корневых сертификатов
   * @param cert сертификат, до которого нужно построить цепочку
   * @return цепочку сертификатов
   * @throws ConfigurerException в случае каких-то ошибок
   */
  public CertPath getCertPath(List<Certificate> rootCerts, Certificate cert)
      throws ConfigurerException {
    try {
      final Set<TrustAnchor> trust = new HashSet<>();

      for (Certificate rootCert : rootCerts) {
        trust.add(new TrustAnchor((X509Certificate) rootCert, null));
      }
      final X509CertSelector selector = new X509CertSelector();
      selector.setCertificate((X509Certificate) cert);

      final PKIXBuilderParameters pkixBuilderParameters = new PKIXBuilderParameters(trust,
          selector);
      pkixBuilderParameters.setSigProvider(null);

      List<Certificate> rootAndUserCerts = new ArrayList<>(rootCerts);
      rootAndUserCerts.add(cert);

      final CollectionCertStoreParameters ccsp = new CollectionCertStoreParameters(
          rootAndUserCerts);
      final CertStore store = CertStore.getInstance("Collection", ccsp);
      pkixBuilderParameters.addCertStore(store);
      pkixBuilderParameters.setRevocationEnabled(false);

      final PKIXCertPathBuilderResult res =
          (PKIXCertPathBuilderResult) CertPathBuilder.
              getInstance("CPPKIX", "RevCheck").build(pkixBuilderParameters);
      return res.getCertPath();
    } catch (Exception ex) {
      logger.error("", ex);
      throw new ConfigurerException(ex.getClass(), ex.getMessage());
    }
  }

  /**
   * Добавляет сертификат в доверенное хранилище сертификатов Java. Добавление выполняется только в
   * том случае, если добавляемого сертификата еще нет в хранилице
   *
   * @param jtksPath путь к хранилищу доверенных сертификатов java
   * @param cert - добавляемый сертификат
   */
  public void addCertificate(String jtksPath, Certificate cert) throws ConfigurerException {
    try {
      KeyStore jtks = KeyStore.getInstance(KeyStore.getDefaultType());
      jtks.load(new FileInputStream(jtksPath), "changeit".toCharArray());
      // проверям, что сертификата еще нет в хранилище
      if (jtks.getCertificateAlias(cert) == null) {
        X509Certificate x509Certificate = (X509Certificate) cert;
        X500Name x500Name = new JcaX509CertificateHolder(x509Certificate).getSubject();
        RDN cn = x500Name.getRDNs(BCStyle.CN)[0];
        // Имя (alias) добавляемого сертификата будет значение поля CN переведенное в транслитерацию
        // и нормализованное (удалены пробельный символы и и всякие апострофы)
        String name = IETFUtils.valueToString(cn.getFirst().getValue());
        String result = Transliterator.getInstance("Russian-Latin/BGN")
            .transliterate(name)
            .replaceAll("[^A-Za-z0-9]", "");
        result = result + "_" + x509Certificate.getSerialNumber().toString();
        logger.info(String.format("Certificate %s will be appended to keystore", result));
        jtks.setCertificateEntry(result, cert);
        jtks.store(new FileOutputStream(jtksPath), "changeit".toCharArray());
        logger.info(String.format("Certificate %s appended successfully to keystore", result));
      }
    } catch (Exception ex) {
      logger.error("", ex);
      throw new ConfigurerException(ex.getClass(), ex.getMessage());
    }
  }

  public String getJTKSPath(@NonNull String jreHome) {
    if (!jreHome.endsWith(File.separator)) {
      jreHome = jreHome.concat(File.separator);
    }
    return (jreHome + "lib/security/cacerts")
        .replaceAll("/", Matcher.quoteReplacement(File.separator));
  }
}
