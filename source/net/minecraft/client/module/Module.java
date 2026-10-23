package net.minecraft.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.value.Value;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class Module {
    protected final String name;
    protected final String description;
    protected final ModuleCategory category;
    protected final int keybind;
    protected boolean enabled;
    protected boolean visible;
    protected final List<Value<?>> values;
    protected Consumer<Module> onToggle;

    public Module(String name, String description, ModuleCategory category) {
        this(name, description, category, 0);
    }

    public Module(String name, String description, ModuleCategory category, int keybind) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.keybind = keybind;
        this.enabled = false;
        this.visible = true;
        this.values = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public int getKeybind() {
        return keybind;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        boolean wasEnabled = this.enabled;
        this.enabled = enabled;
        if (enabled != wasEnabled) {
            if (enabled) {
                onEnable();
            } else {
                onDisable();
            }
            if (onToggle != null) {
                onToggle.accept(this);
            }
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public List<Value<?>> getValues() {
        return values;
    }

    public void addValue(Value<?> value) {
        values.add(value);
    }

    public <T extends Value<?>> T registerValue(T value) {
        addValue(value);
        return value;
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    public void onTick() {
    }

    public void onRender() {
    }

    public void onKeyPress(int key) {
    }

    public Component getDisplayName() {
        return Component.literal(name);
    }

    public void setOnToggle(Consumer<Module> onToggle) {
        this.onToggle = onToggle;
    }
}