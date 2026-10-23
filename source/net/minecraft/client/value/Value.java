package net.minecraft.client.value;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public abstract class Value<T> {
    protected final String name;
    protected final String description;
    protected T value;
    protected T defaultValue;
    protected Consumer<T> onChanged;

    public Value(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
        if (onChanged != null) {
            onChanged.accept(value);
        }
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public void setOnChanged(Consumer<T> onChanged) {
        this.onChanged = onChanged;
    }

    public abstract Component getDisplayValue();

    public abstract void cycleForward();

    public abstract void cycleBackward();

    public void reset() {
        setValue(defaultValue);
    }

    public boolean isDefault() {
        return value.equals(defaultValue);
    }
}