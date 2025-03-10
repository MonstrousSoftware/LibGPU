package com.monstrous.jlay;

import com.monstrous.graphics.Color;
import com.monstrous.jlay.utils.Event;


public class Button extends Box {
    public float radius = 16f;
    public float dropX = 0;
    public float dropY = 0;
    private final Color savedColor;

    public Button() {
        savedColor = new Color(Color.WHITE);
        addListener(event -> {
            if (event == Event.CLICKED)
                setColor(Color.RED);
            else if (event == Event.MOUSE_ENTERS) {
                savedColor.set(color);
                setColor(Color.YELLOW);
            }
            else if (event == Event.MOUSE_EXITS) {
                setColor(savedColor);
            }
            return false;
        });
    }

}
