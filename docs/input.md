## Input


### Polling
Key status can be retrieved using ```isKeyPressed(keycode)``` where keycode is one of the values from Input.Keys.

Mouse position can be retrieved using `getX()` and `getY()`.

### Input events

Input events can be received by defining an InputProcessor and registering this using `LibGPU.input.setInputProcessor(processor)`.
The input processor will be updated on events by calls to its methods:
- ```boolean keyDown(int keycode)```
- ```boolean keyUp(int keycode)```
- ```boolean mouseMove(float x, float y)```
- ```boolean scrolled(float x, float y)```
