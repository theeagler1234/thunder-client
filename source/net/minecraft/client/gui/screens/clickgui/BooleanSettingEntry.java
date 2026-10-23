package net.minecraft.client.gui.screens.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class BooleanSettingEntry extends SettingEntry {
    private final BooleanValue boolValue;
    private boolean dragging = false;

    public BooleanSettingEntry(BooleanValue value, int x, int y, int width, int height) {
        super(value, x, y, width, height);
        this.boolValue = value;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        int bgColor = hovered ? 0x60333333 : 0x40222222;
        int toggleColor = boolValue.getValue() ? 0xFF00FF00 : 0xFF555555;
        int textColor = 0xFFFFFFFF;

        guiGraphics.fill(x, y, x + width, y + height, bgColor);

        // Label
        renderLabel(guiGraphics, value.getName(), x + 5, y + (height - 8) / 2, textColor);

        // Toggle switch
        int toggleX = x + width - 35;
        int toggleY = y + (height - 14) / 2;
        int toggleWidth = 30;
        int toggleHeight = 14;

        // Track
        guiGraphics.fill(toggleX, toggleY, toggleX + toggleWidth, toggleY + toggleHeight,
            boolValue.getValue() ? 0xFF00AA00 : 0xFF444444);

        // Knob
        int knobX = toggleX + (boolValue.getValue() ? toggleWidth - 12 : 2);
        guiGraphics.fill(knobX, toggleY + 2, knobX + 10, toggleY + toggleHeight - 2, 0xFFFFFFFF);

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!hovered || button != 0) return false;
        boolValue.toggle();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }
}