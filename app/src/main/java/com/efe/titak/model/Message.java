package com.efe.titak.model;

import java.io.Serializable;

public class Message implements Serializable {
    private String text;
    private boolean isFromBot;

    public Message(String text, boolean isFromBot) {
        this.text = text;
        this.isFromBot = isFromBot;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isFromBot() {
        return isFromBot;
    }

    public void setFromBot(boolean fromBot) {
        isFromBot = fromBot;
    }
}