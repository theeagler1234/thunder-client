package net.minecraft.client.value;

import net.minecraft.network.chat.Component;

public class StringValue extends Value<String> {
    private final int maxLength;
    private String unsavedValue;

    public StringValue(String name, String description, String defaultValue) {
        this(name, description, defaultValue, 100);
    }

    public StringValue(String name, String description, String defaultValue, int maxLength) {
        super(name, description, defaultValue);
        this.maxLength = maxLength;
        this.unsavedValue = defaultValue;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public String getUnsavedValue() {
        return unsavedValue;
    }

    public void setUnsavedValue(String value) {
        this.unsavedValue = value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    public void applyUnsavedValue() {
        setValue(unsavedValue);
    }

    @Override
    public Component getDisplayValue() {
        String value = getValue();
        return Component.literal(value.isEmpty() ? "(empty)" : value);
    }

    @Override
    public void cycleForward() {
        // No-op for string values
    }

    @Override
    public void cycleBackward() {
        // No-op for string values
    }
}