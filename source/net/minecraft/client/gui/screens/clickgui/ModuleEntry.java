package net.minecraft.client.gui.screens.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.module.Module;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

public class ModuleEntry {
    public final Module module;
    private final CategoryPanel panel;
    private int x, y, width, height;
    private boolean hovered = false;

    public ModuleEntry(Module module, CategoryPanel panel, int x, int y, int width, int height) {
        this.module = module;
        this.panel = panel;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        int bgColor = hovered ? 0x80333333 : 0x40222222;
        int textColor = module.isEnabled() ? 0xFF00FF00 : 0xFFFFFFFF;
        int accentColor = module.getCategory().getColor();

        // Background
        guiGraphics.fill(x, y, x + width, y + height, bgColor);

        // Enabled indicator
        if (module.isEnabled()) {
            guiGraphics.fill(x, y, x + 3, y + height, accentColor);
        }

        // Module name
        String name = module.getName();
        int textX = x + 8;
        int textY = y + (height - 8) / 2;
        guiGraphics.drawString(Minecraft.getInstance().font, name, textX, textY, textColor, true);

        // Keybind hint
        if (module.getKeybind() != 0) {
            String keyName = Minecraft.getInstance().options.keyMappings.get(module.getKeybind()) != null
                ? Minecraft.getInstance().options.keyMappings.get(module.getKeybind()).getTranslatedKeyMessage().getString()
                : "Key(" + module.getKeybind() + ")";
            String keyText = "[" + keyName + "]";
            int keyWidth = Minecraft.getInstance().font.width(keyText);
            guiGraphics.drawString(Minecraft.getInstance().font, keyText,
                x + width - keyWidth - 5, textY, 0x888888, true);
        }

        // Settings indicator
        if (!module.getValues().isEmpty()) {
            String settingsText = "⚙";
            int settingsWidth = Minecraft.getInstance().font.width(settingsText);
            int settingsX = x + width - settingsWidth - (module.getKeybind() != 0 ? 25 : 5);
            guiGraphics.drawString(Minecraft.getInstance().font, settingsText, settingsX, textY, 0x88AAFF, true);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!hovered) return false;

        if (button == 0) { // Left click - toggle
            module.toggle();
            return true;
        } else if (button == 1) { // Right click - open settings
            if (!module.getValues().isEmpty()) {
                panel.openSettings(module);
            }
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }
}