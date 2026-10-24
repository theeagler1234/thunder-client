package net.minecraft.client.gui.screens.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.value.IntegerValue;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class IntegerSettingEntry extends SettingEntry {
    private final IntegerValue intValue;
    private boolean dragging = false;

    public IntegerSettingEntry(IntegerValue value, int x, int y, int width, int height) {
        super(value, x, y, width, height);
        this.intValue = value;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        int bgColor     = hovered ? 0x60333333 : 0x40222222;
        int accentColor = 0xFF00AAFF;
        int textColor   = 0xFFFFFFFF;

        guiGraphics.fill(x, y, x + width, y + height, bgColor);

        String label = value.getName() + ": " + intValue.getValue();
        renderLabel(guiGraphics, label, x + 5, y + (height - 8) / 2, textColor);

        int trackX      = x + 5;
        int trackY      = y + height - 6;
        int trackWidth  = width - 10;
        int trackHeight = 4;

        guiGraphics.fill(trackX, trackY, trackX + trackWidth, trackY + trackHeight, 0xFF333333);

        double percentage = intValue.getPercentage();
        int fillWidth = (int) (trackWidth * percentage);
        guiGraphics.fill(trackX, trackY, trackX + fillWidth, trackY + trackHeight, accentColor);

        int knobX = trackX + fillWidth - 4;
        guiGraphics.fill(knobX, trackY - 3, knobX + 8, trackY + trackHeight + 3, 0xFFFFFFFF);

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!hovered || button != 0) return false;

        int trackX     = x + 5;
        int trackWidth = width - 10;
        double relativeX = Mth.clamp((mouseX - trackX) / trackWidth, 0.0, 1.0);
        intValue.setFromPercentage(relativeX);
        dragging = true;
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            int trackX     = x + 5;
            int trackWidth = width - 10;
            double relativeX = Mth.clamp((mouseX - trackX) / trackWidth, 0.0, 1.0);
            intValue.setFromPercentage(relativeX);
            return true;
        }
        return false;
    }
}
