package net.minecraft.client.value;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class FloatValue extends Value<Float> {
    protected final float min;
    protected final float max;
    protected final float step;
    protected float unsavedValue;

    public FloatValue(String name, String description, float defaultValue, float min, float max) {
        this(name, description, defaultValue, min, max, 0.1f);
    }

    public FloatValue(String name, String description, float defaultValue, float min, float max, float step) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
        this.unsavedValue = defaultValue;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public float getStep() {
        return step;
    }

    @Override
    public Float getValue() {
        return super.getValue();
    }

    @Override
    public void setValue(Float value) {
        super.setValue(Mth.clamp(value, min, max));
    }

    public float getUnsavedValue() {
        return unsavedValue;
    }

    public void setUnsavedValue(float value) {
        this.unsavedValue = Mth.clamp(value, min, max);
    }

    public void applyUnsavedValue() {
        setValue(unsavedValue);
    }

    @Override
    public Component getDisplayValue() {
        return Component.literal(String.format("%.1f", getValue()));
    }

    @Override
    public void cycleForward() {
        setValue(getValue() + step);
    }

    @Override
    public void cycleBackward() {
        setValue(getValue() - step);
    }

    public float getPercentage() {
        return (getValue() - min) / (max - min);
    }

    public void setFromPercentage(float percentage) {
        setValue(min + percentage * (max - min));
    }
}