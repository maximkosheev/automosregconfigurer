package net.monsterdev.automosregconfigurer.exceptions;

public class ConfigurerException extends Exception {
    private static final String ERROR_MESSAGE = "Error while working with java trusted key store %s: %s";

    public ConfigurerException(String causeMessage) {
        super(String.format(ERROR_MESSAGE, "Отсутствует", causeMessage));
    }

    public ConfigurerException(Class<? extends Throwable> causeClass, String causeMessage) {
        super(String.format(ERROR_MESSAGE, causeClass, causeMessage));
    }
}
