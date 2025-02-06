package com.monstrous;

public abstract class Game implements ApplicationListener {
        protected Screen screen;

        @Override
        public void dispose () {
            if (screen != null) screen.hide();
        }

        @Override
        public void pause () {
            if (screen != null) screen.pause();
        }

        @Override
        public void resume () {
            if (screen != null) screen.resume();
        }

        @Override
        public void render () {
            if (screen != null) screen.render(LibGPU.graphics.getDeltaTime());
        }

        @Override
        public void resize (int width, int height) {
            if (screen != null) screen.resize(width, height);
        }

        public void setScreen (Screen screen) {
            if (this.screen != null) this.screen.hide();
            this.screen = screen;
            if (this.screen != null) {
                this.screen.show();
                this.screen.resize(LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
            }
        }

        public Screen getScreen () {
            return screen;
        }
}
