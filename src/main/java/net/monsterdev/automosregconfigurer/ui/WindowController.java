package net.monsterdev.automosregconfigurer.ui;

import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public interface WindowController extends UIController {
    /**
     * Возвращает сцену, в которой открыто окно, с которомым связан данный контроллер
     * @return
     */
    Stage getStage();

    default void close() {
        getStage().fireEvent(new WindowEvent(getStage(), WindowEvent.WINDOW_CLOSE_REQUEST));
    }
}
