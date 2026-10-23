package net.minecraft.client.gui.screens.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.value.Value;
import net.minecraft.network.chat.Component;

public abstract class SettingEntry {
    protected final Value<?> value;
    protected int x, y, width, height;
    protected boolean hovered = false;
    protected boolean focused = false;

    public SettingEntry(Value<?> value, int x, int y, int width, int height) {
        this.value = value;
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

    public abstract void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);
    public abstract boolean mouseClicked(double mouseX, double mouseY, int button);
    public abstract boolean mouseReleased(double mouseX, double mouseY, int button);
    public abstract boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY);

    protected void renderLabel(GuiGraphics guiGraphics, String label, int x, int y, int color) {
        guiGraphics.drawString(guiGraphics.getFont(), label, x, y, color, true);
    }

    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (hovered && value.getDescription() != null && !value.getDescription().isEmpty()) {
            guiGraphics.renderTooltip(guiGraphics.getFont(), Component.literal(value.getDescription()), mouseX, mouseY);
        }
    }
}