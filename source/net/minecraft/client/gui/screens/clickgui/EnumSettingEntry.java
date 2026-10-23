package net.minecraft.client.gui.screens.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.value.EnumValue;
import net.minecraft.network.chat.Component;

import java.util.List;

public class EnumSettingEntry extends SettingEntry {
    private final EnumValue<?> enumValue;
    private boolean dropdownOpen = false;
    private int dropdownHeight = 0;

    public EnumSettingEntry(EnumValue<?> value, int x, int y, int width, int height) {
        super(value, x, y, width, height);
        this.enumValue = value;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        int bgColor = hovered ? 0x60333333 : 0x40222222;
        int textColor = 0xFFFFFFFF;
        int accentColor = 0xFF00AAFF;

        guiGraphics.fill(x, y, x + width, y + height, bgColor);

        // Label
        String label = value.getName() + ": " + enumValue.getValue().name();
        renderLabel(guiGraphics, label, x + 5, y + (height - 8) / 2, textColor);

        // Dropdown arrow
        String arrow = dropdownOpen ? "▲" : "▼";
        int arrowWidth = Minecraft.getInstance().font.width(arrow);
        int arrowX = x + width - arrowWidth - 5;
        renderLabel(guiGraphics, arrow, arrowX, y + (height - 8) / 2, accentColor);

        // Dropdown
        if (dropdownOpen) {
            List<?> values = enumValue.getValues();
            dropdownHeight = values.size() * 20;

            int dropX = x;
            int dropY = y + height;
            int dropWidth = width;

            // Background
            guiGraphics.fill(dropX, dropY, dropX + dropWidth, dropY + dropdownHeight, 0xFF1A1A1A);
            guiGraphics.fill(dropX - 1, dropY - 1, dropX + dropWidth + 1, dropY + dropdownHeight + 1, 0xFF333333);

            for (int i = 0; i < values.size(); i++) {
                Object val = values.get(i);
                int itemY = dropY + i * 20;
                boolean isSelected = val == enumValue.getValue();
                boolean isHovered = mouseX >= dropX && mouseX <= dropX + dropWidth && mouseY >= itemY && mouseY <= itemY + 20;

                if (isSelected || isHovered) {
                    guiGraphics.fill(dropX, itemY, dropX + dropWidth, itemY + 20, isSelected ? 0x8000AAFF : 0x60333333);
                }

                renderLabel(guiGraphics, val.toString(), dropX + 5, itemY + 6, textColor);
            }
        }

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (dropdownOpen) {
            List<?> values = enumValue.getValues();
            int dropX = x;
            int dropY = y + height;
            int dropWidth = width;

            if (mouseX >= dropX && mouseX <= dropX + dropWidth && mouseY >= dropY && mouseY <= dropY + dropdownHeight) {
                int index = (int) ((mouseY - dropY) / 20);
                if (index >= 0 && index < values.size()) {
                    enumValue.setByIndex(index);
                }
                dropdownOpen = false;
                return true;
            } else {
                dropdownOpen = false;
                return true;
            }
        }

        if (hovered) {
            dropdownOpen = !dropdownOpen;
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    @Override
    public int getHeight() {
        return dropdownOpen ? super.getHeight() + dropdownHeight : super.getHeight();
    }
}