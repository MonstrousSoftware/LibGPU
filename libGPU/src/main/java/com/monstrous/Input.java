package com.monstrous;

import static org.lwjgl.glfw.GLFW.*;

public class Input {

    static public class Keys{
        public static final int ANY_KEY = -1,
                SPACE         = 32,
                APOSTROPHE    = 39,
                COMMA         = 44,
                MINUS         = 45,
                PERIOD        = 46,
                SLASH         = 47,
                NUM_0             = 48,
                NUM_1             = 49,
                NUM_2             = 50,
                NUM_3             = 51,
                NUM_4             = 52,
                NUM_5             = 53,
                NUM_6             = 54,
                NUM_7             = 55,
                NUM_8             = 56,
                NUM_9             = 57,
                SEMICOLON     = 59,
                EQUAL         = 61,
                A             = 65,
                B             = 66,
                C             = 67,
                D             = 68,
                E             = 69,
                F             = 70,
                G             = 71,
                H             = 72,
                I             = 73,
                J             = 74,
                K             = 75,
                L             = 76,
                M             = 77,
                N             = 78,
                O             = 79,
                P             = 80,
                Q             = 81,
                R             = 82,
                S             = 83,
                T             = 84,
                U             = 85,
                V             = 86,
                W             = 87,
                X             = 88,
                Y             = 89,
                Z             = 90,
                LEFT_BRACKET  = 91,
                BACKSLASH     = 92,
                RIGHT_BRACKET = 93,
                GRAVE_ACCENT  = 96,
                WORLD_1       = 161,
                WORLD_2       = 162,
                ESCAPE        = 256,
                ENTER         = 257,
                TAB           = 258,
                BACKSPACE     = 259,
                INSERT        = 260,
                DELETE        = 261,
                RIGHT         = 262,
                LEFT          = 263,
                DOWN          = 264,
                UP            = 265,
                PAGE_UP       = 266,
                PAGE_DOWN     = 267,
                HOME          = 268,
                END           = 269,
                CAPS_LOCK     = 280,
                SCROLL_LOCK   = 281,
                NUM_LOCK      = 282,
                PRINT_SCREEN  = 283,
                PAUSE         = 284,
                F1            = 290,
                F2            = 291,
                F3            = 292,
                F4            = 293,
                F5            = 294,
                F6            = 295,
                F7            = 296,
                F8            = 297,
                F9            = 298,
                F10           = 299,
                F11           = 300,
                F12           = 301,
                F13           = 302,
                F14           = 303,
                F15           = 304,
                F16           = 305,
                F17           = 306,
                F18           = 307,
                F19           = 308,
                F20           = 309,
                F21           = 310,
                F22           = 311,
                F23           = 312,
                F24           = 313,
                F25           = 314,
                KP_0          = 320,
                KP_1          = 321,
                KP_2          = 322,
                KP_3          = 323,
                KP_4          = 324,
                KP_5          = 325,
                KP_6          = 326,
                KP_7          = 327,
                KP_8          = 328,
                KP_9          = 329,
                KP_DECIMAL    = 330,
                KP_DIVIDE     = 331,
                KP_MULTIPLY   = 332,
                KP_SUBTRACT   = 333,
                KP_ADD        = 334,
                KP_ENTER      = 335,
                KP_EQUAL      = 336,
                LEFT_SHIFT    = 340,
                LEFT_CONTROL  = 341,
                LEFT_ALT      = 342,
                LEFT_SUPER    = 343,
                RIGHT_SHIFT   = 344,
                RIGHT_CONTROL = 345,
                RIGHT_ALT     = 346,
                RIGHT_SUPER   = 347,
                MENU          = 348,
                LAST          = MENU;

    }

    static public class Buttons{
        public static final int     LEFT = 0,
                                    RIGHT = 1,
                                    MIDDLE = 2;
    }

    private InputProcessor processor;
    private final boolean[] isKeyPressed = new boolean[Input.Keys.LAST+1];
    private final boolean[] isMouseButtonPressed = new boolean[GLFW_MOUSE_BUTTON_LAST+1];
    private int pressedKeyCount;
    private float mouseX, mouseY;
    private int mousePressed;

    public void setInputProcessor (InputProcessor processor){
        this.processor = processor;
    }

    public InputProcessor getInputProcessor() {
        return processor;
    }

    public boolean isKeyPressed(int keyCode){
        if(keyCode == Keys.ANY_KEY)
            return pressedKeyCount > 0;
        return isKeyPressed[keyCode];
    }

    public boolean isButtonPressed(int buttonCode){
        return isMouseButtonPressed[buttonCode];
    }

    public float getX(){
        return mouseX;
    }

    public float getY(){
        return mouseY;
    }

    public void processMouseMove(int x, int y){
        mouseX = x;
        mouseY = y;
        if(LibGPU.input.processor != null) {
            if(mousePressed == 0)
                LibGPU.input.processor.mouseMoved(x, y);
            else
                LibGPU.input.processor.touchDragged(x, y, 0);
        }
    }

    public void processMouseEvent(int x, int y, int button, int action){
        mouseX = x;
        mouseY = y;

        if (action == GLFW_PRESS) {
            mousePressed++;
            isMouseButtonPressed[button] = true;
            if(LibGPU.input.processor != null)
                LibGPU.input.processor.touchDown(x, y, 0, button);
        }
        else if (action == GLFW_RELEASE) {
            mousePressed = Math.max(0, mousePressed-1);
            isMouseButtonPressed[button] = false;
            if(LibGPU.input.processor != null)
                LibGPU.input.processor.touchUp(x, y, 0, button);
        }

    }

    public void processScroll(float x, float y){
        if(LibGPU.input.processor != null)
            LibGPU.input.processor.scrolled( x, y);
    }

    public void processKeyEvent(int glfwKey, int action){
        int key = convertFromGLFW(glfwKey);
        if(action == GLFW_PRESS){
            pressedKeyCount++;
            isKeyPressed[key] = true;
            if(processor != null) {
                processor.keyDown(key);
                char character = characterForKeyCode(key);
                if(character != 0)
                    processor.keyTyped(character);
            }
        }
        else if(action == GLFW_RELEASE) {
            pressedKeyCount--;
            isKeyPressed[key] = false;
            if(processor != null)
                processor.keyUp(key);
        }
    }

    public void processCharEvent(int codepoint){
        if(processor != null)
            processor.keyTyped((char)codepoint);
    }

    private char characterForKeyCode(int keycode ){
        switch (keycode) {
            case Keys.BACKSPACE:
                return 8;
            case Keys.TAB:
                return '\t';
            case Keys.ENTER:
                return '\n';
        }
        return 0;
    }


    private int convertFromGLFW(int key){
        return key; // since we use the same code values, the mapping is 1 to 1
    }
}
