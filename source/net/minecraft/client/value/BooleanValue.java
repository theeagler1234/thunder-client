package net.minecraft.client.value;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;

public class BooleanValue extends Value<Boolean> {
    public BooleanValue(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
    }

    @Override
    public Component getDisplayValue() {
        return getValue() ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
    }

    @Override
    public void cycleForward() {
        setValue(!getValue());
    }

    @Override
    public void cycleBackward() {
        setValue(!getValue());
    }

    public void toggle() {
        setValue(!getValue());
    }
}