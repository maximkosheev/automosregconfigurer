package net.monsterdev.automosregconfigurer.ui;

import javafx.scene.Node;

import java.net.URL;
import java.util.ResourceBundle;

public class AbstractUIController implements UIController {
    private Node root;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    @Override
    public Node getView() {
        return root;
    }

    @Override
    public void setView(Node node) {
        root = node;
    }
}
