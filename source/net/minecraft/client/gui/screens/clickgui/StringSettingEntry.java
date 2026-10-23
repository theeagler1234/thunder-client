package net.minecraft.client.gui.screens.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.value.StringValue;
import net.minecraft.network.chat.Component;

public class StringSettingEntry extends SettingEntry {
    private final StringValue stringValue;
    private boolean editing = false;
    private String buffer = "";

    public StringSettingEntry(StringValue value, int x, int y, int width, int height) {
        super(value, x, y, width, height);
        this.stringValue = value;
        this.buffer = value.getUnsavedValue();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        int bgColor = editing ? 0x8000AAFF : (hovered ? 0x60333333 : 0x40222222);
        int textColor = 0xFFFFFFFF;
        int accentColor = 0xFF00AAFF;

        guiGraphics.fill(x, y, x + width, y + height, bgColor);

        // Label
        String displayValue = editing ? buffer + (System.currentTimeMillis() / 500 % 2 == 0 ? "|" : "") : stringValue.getValue();
        if (displayValue.isEmpty() && !editing) displayValue = "(empty)";

        String label = value.getName() + ": " + displayValue;
        renderLabel(guiGraphics, label, x + 5, y + (height - 8) / 2, editing ? accentColor : textColor);

        // Edit hint
        if (!editing && hovered) {
            String hint = "Click to edit";
            int hintWidth = Minecraft.getInstance().font.width(hint);
            renderLabel(guiGraphics, hint, x + width - hintWidth - 5, y + (height - 8) / 2, 0x888888);
        }

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!hovered || button != 0) {
            if (editing && button != 0) {
                finishEditing();
            }
            editing = false;
            return false;
        }

        if (!editing) {
            editing = true;
            buffer = stringValue.getUnsavedValue();
        }
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

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!editing) return false;

        if (keyCode == 256) { // ESC
            cancelEditing();
            return true;
        } else if (keyCode == 257) { // ENTER
            finishEditing();
            return true;
        } else if (keyCode == 259) { // BACKSPACE
            if (!buffer.isEmpty()) {
                buffer = buffer.substring(0, buffer.length() - 1);
            }
            return true;
        }

        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (!editing) return false;

        if (codePoint >= 32 && codePoint < 127 && buffer.length() < stringValue.getMaxLength()) {
            buffer += codePoint;
            return true;
        }
        return false;
    }

    private void finishEditing() {
        stringValue.setUnsavedValue(buffer);
        stringValue.applyUnsavedValue();
        editing = false;
    }

    private void cancelEditing() {
        buffer = stringValue.getValue();
        editing = false;
    }
}