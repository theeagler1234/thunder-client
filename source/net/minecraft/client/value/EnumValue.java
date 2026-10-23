package net.minecraft.client.value;

import java.util.List;
import net.minecraft.network.chat.Component;

public class EnumValue<T extends Enum<T>> extends Value<T> {
    protected final List<T> values;
    protected int index;

    public EnumValue(String name, String description, T defaultValue, List<T> values) {
        super(name, description, defaultValue);
        this.values = List.copyOf(values);
        this.index = values.indexOf(defaultValue);
    }

    public List<T> getValues() {
        return values;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public void setValue(T value) {
        super.setValue(value);
        this.index = values.indexOf(value);
    }

    @Override
    public Component getDisplayValue() {
        return Component.literal(getValue().name());
    }

    @Override
    public void cycleForward() {
        index = (index + 1) % values.size();
        setValue(values.get(index));
    }

    @Override
    public void cycleBackward() {
        index = (index - 1 + values.size()) % values.size();
        setValue(values.get(index));
    }

    public void setByIndex(int index) {
        if (index >= 0 && index < values.size()) {
            this.index = index;
            setValue(values.get(index));
        }
    }
}