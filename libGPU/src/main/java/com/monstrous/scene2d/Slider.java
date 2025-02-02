package com.monstrous.scene2d;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.utils.Disposable;

public class Slider extends Widget implements Disposable {

    private Texture textureSliderBg;  // should be static & shared (and from an atlas)
    private Texture textureKnob;
    private Color color;
    private WrappedFloat controlledValue;
    private float min, max, step;

    public Slider(WrappedFloat controlledValue, float min, float max, float step) {
        this.min = min;
        this.max = max;
        this.step = step;
        this.controlledValue = controlledValue != null ? controlledValue : new WrappedFloat((max+min)/2f);

        textureSliderBg = new Texture("guiElements/slider_bg.png");
        textureKnob = new Texture("guiElements/slider_knob.png");

        setPreferredSize(100, textureKnob.getHeight());

        this.color = new Color(Color.WHITE);
    }

    public void setColor( Color color ){
        this.color.set(color);
    }


    @Override
    public void draw(SpriteBatch batch){
        batch.setColor(color);
        batch.draw( textureSliderBg, x+parentCell.x, y+parentCell.y, w, h);
        int sliderX = (int) ((w-textureKnob.getWidth()) * (controlledValue.value - min)/(max-min));
        batch.draw( textureKnob, x+parentCell.x + sliderX, y+parentCell.y, textureKnob.getWidth(), textureKnob.getHeight());
    }


    @Override
    public void onDrag(int mx, int my){
        mx -= (x + parentCell.x);
        my -= (y + parentCell.y);

        //System.out.println("slider drag "+mx+", "+my);

        // convert mouse position to slider value
        float sliderValue = (max-min)*((float) mx /w);
        sliderValue = min + (step * Math.round(sliderValue/step));
        controlledValue.value = Math.max(min, Math.min(sliderValue, max));
    }


    @Override
    public void dispose() {
        textureSliderBg.dispose();
        textureKnob.dispose();
    }
}
