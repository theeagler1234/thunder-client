package net.minecraft.client.module;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public enum ModuleCategory {
    COMBAT("Combat", "⚔", -65536),
    MOVEMENT("Movement", "⚡", -256),
    RENDER("Render", "🎨", -16711936),
    PLAYER("Player", "👤", -16776961),
    WORLD("World", "🌍", -8355712),
    MISC("Misc", "⚙", -39424);

    private final String name;
    private final String icon;
    private final int color;

    ModuleCategory(String name, String icon, int color) {
        this.name = name;
        this.icon = icon;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public int getColor() {
        return color;
    }

    public Component getDisplayName() {
        return Component.literal(icon + " " + name);
    }
}