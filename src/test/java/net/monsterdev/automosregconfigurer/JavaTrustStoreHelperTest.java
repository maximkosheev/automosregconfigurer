package net.monsterdev.automosregconfigurer;

import org.junit.Before;

public class JavaTrustStoreHelperTest {
    @Before
    public void initialize() {
        System.setProperty("com.sun.security.enableCRLDP", "true"); // для проверки по CRL DP
        System.setProperty("com.sun.security.enableAIAcaIssuers", "true"); // для загрузки сертификатов по AIA из сети
        System.setProperty("ru.CryptoPro.reprov.enableAIAcaIssuers", "true"); // для загрузки сертификатов по AIA из сети
    }
}
