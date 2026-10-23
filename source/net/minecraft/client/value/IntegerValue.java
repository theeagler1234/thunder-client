package net.minecraft.client.value;

import net.minecraft.network.chat.Component;

public class IntegerValue extends Value<Integer> {
    private final int min;
    private final int max;
    private int unsavedValue;

    public IntegerValue(String name, String description, int defaultValue, int min, int max) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.unsavedValue = defaultValue;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    @Override
    public Integer getValue() {
        return super.getValue();
    }

    @Override
    public void setValue(Integer value) {
        super.setValue(Math.max(min, Math.min(max, value)));
    }

    public int getUnsavedValue() {
        return unsavedValue;
    }

    public void setUnsavedValue(int value) {
        this.unsavedValue = Math.max(min, Math.min(max, value));
    }

    public void applyUnsavedValue() {
        setValue(unsavedValue);
    }

    @Override
    public Component getDisplayValue() {
        return Component.literal(String.valueOf(getValue()));
    }

    @Override
    public void cycleForward() {
        setValue(getValue() + 1);
    }

    @Override
    public void cycleBackward() {
        setValue(getValue() - 1);
    }

    public double getPercentage() {
        return (double) (getValue() - min) / (max - min);
    }

    public void setFromPercentage(double percentage) {
        setValue(min + (int) (percentage * (max - min)));
    }
}