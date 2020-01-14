package net.monsterdev.automosregconfigurer.model;

import lombok.Data;

import java.security.cert.Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
public class CertificateInfo {
    /**
     * Тип хранилища ключа, например, RuToken, EToken и т.д.
     * Данное приложение работает только с поставщиком JCP.
     * Т.е. поставщик создается только так Security.getProvider("JCP")
     */
    private String name;
    private Date validFrom;
    private Date validTo;
    private Certificate certificate;

    public String getValidity() {
        DateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        return format.format(validFrom) + " - " + format.format(validTo);
    }
}
