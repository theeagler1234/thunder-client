package net.minecraft.client.gui.screens.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.value.Value;
import net.minecraft.network.chat.Component;

public class GenericSettingEntry extends SettingEntry {
    public GenericSettingEntry(Value<?> value, int x, int y, int width, int height) {
        super(value, x, y, width, height);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        int bgColor = hovered ? 0x60333333 : 0x40222222;
        int textColor = 0xFFFFFFFF;

        guiGraphics.fill(x, y, x + width, y + height, bgColor);

        String label = value.getName() + ": " + value.getDisplayValue().getString();
        renderLabel(guiGraphics, label, x + 5, y + (height - 8) / 2, textColor);

        // Cycle hint
        if (hovered) {
            String hint = "[Click to cycle]";
            int hintWidth = Minecraft.getInstance().font.width(hint);
            renderLabel(guiGraphics, hint, x + width - hintWidth - 5, y + (height - 8) / 2, 0x88AAFF);
        }

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!hovered || button != 0) return false;
        value.cycleForward();
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