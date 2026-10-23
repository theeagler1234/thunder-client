package net.minecraft.client.gui.screens.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.value.ColorValue;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public class ColorSettingEntry extends SettingEntry {
    private final ColorValue colorValue;
    private boolean draggingHue = false;
    private boolean draggingSatVal = false;
    private boolean draggingAlpha = false;
    private boolean draggingRainbowSpeed = false;
    private boolean showPicker = false;

    private static final int PICKER_WIDTH = 150;
    private static final int PICKER_HEIGHT = 100;
    private static final int HUE_HEIGHT = 15;
    private static final int ALPHA_HEIGHT = 15;
    private static final int SPEED_HEIGHT = 15;

    public ColorSettingEntry(ColorValue value, int x, int y, int width, int height) {
        super(value, x, y, width, height);
        this.colorValue = value;
        this.height = showPicker ? height + PICKER_HEIGHT + 20 : height;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        int bgColor = hovered ? 0x60333333 : 0x40222222;
        int textColor = 0xFFFFFFFF;

        guiGraphics.fill(x, y, x + width, y + height, bgColor);

        // Label
        String label = value.getName() + ": " + value.getDisplayValue().getString();
        renderLabel(guiGraphics, label, x + 5, y + (height - 8) / 2, textColor);

        // Color preview
        int previewX = x + width - 55;
        int previewY = y + (height - 16) / 2;
        int previewSize = 16;

        if (colorValue.isRainbow()) {
            // Rainbow preview - animated gradient
            for (int i = 0; i < previewSize; i++) {
                float hue = (System.currentTimeMillis() / 1000.0f + i * 0.1f) % 1.0f;
                int color = ARGB.colorFromHSV(hue, 1.0f, 1.0f);
                guiGraphics.fill(previewX + i, previewY, previewX + i + 1, previewY + previewSize, color);
            }
        } else {
            guiGraphics.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, colorValue.getColor());
        }

        // Border
        guiGraphics.fill(previewX - 1, previewY - 1, previewX + previewSize + 1, previewY, 0xFF000000);
        guiGraphics.fill(previewX - 1, previewY + previewSize, previewX + previewSize + 1, previewY + previewSize + 1, 0xFF000000);
        guiGraphics.fill(previewX - 1, previewY, previewX, previewY + previewSize, 0xFF000000);
        guiGraphics.fill(previewX + previewSize, previewY, previewX + previewSize + 1, previewY + previewSize, 0xFF000000);

        // Rainbow toggle button
        int toggleX = previewX - 55;
        int toggleY = previewY;
        String rainbowText = colorValue.isRainbow() ? "🌈" : "🎨";
        int toggleWidth = Minecraft.getInstance().font.width(rainbowText) + 10;
        guiGraphics.fill(toggleX, toggleY, toggleX + toggleWidth, toggleY + previewSize, 0xFF333333);
        renderLabel(guiGraphics, rainbowText, toggleX + 5, toggleY + 2, textColor);

        if (showPicker) {
            renderColorPicker(guiGraphics, mouseX, mouseY);
        }

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderColorPicker(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int pickerX = x + 5;
        int pickerY = y + height - PICKER_HEIGHT - 25;

        // Background
        guiGraphics.fill(pickerX - 2, pickerY - 2, pickerX + PICKER_WIDTH + 2, pickerY + PICKER_HEIGHT + HUE_HEIGHT + ALPHA_HEIGHT + SPEED_HEIGHT + 20, 0xFF1A1A1A);
        guiGraphics.fill(pickerX - 1, pickerY - 1, pickerX + PICKER_WIDTH + 1, pickerY + PICKER_HEIGHT + HUE_HEIGHT + ALPHA_HEIGHT + SPEED_HEIGHT + 19, 0xFF333333);

        // Saturation/Value square
        int svX = pickerX;
        int svY = pickerY;
        int svSize = PICKER_WIDTH;

        // Draw SV square
        float[] hsv = colorValue.getHSV();
        for (int i = 0; i < svSize; i++) {
            for (int j = 0; j < svSize; j++) {
                float s = (float) i / svSize;
                float v = 1.0f - (float) j / svSize;
                int color = ARGB.colorFromHSV(hsv[0], s, v);
                guiGraphics.fill(svX + i, svY + j, svX + i + 1, svY + j + 1, color);
            }
        }

        // SV selector
        int selectorX = svX + (int) (hsv[1] * svSize) - 3;
        int selectorY = svY + (int) ((1.0f - hsv[2]) * svSize) - 3;
        guiGraphics.fill(selectorX, selectorY, selectorX + 7, selectorY + 7, 0xFFFFFFFF);
        guiGraphics.fill(selectorX + 1, selectorY + 1, selectorX + 6, selectorY + 6, 0xFF000000);

        // Hue slider
        int hueX = pickerX;
        int hueY = svY + svSize + 5;
        int hueWidth = PICKER_WIDTH;

        for (int i = 0; i < hueWidth; i++) {
            float hue = (float) i / hueWidth;
            int color = ARGB.colorFromHSV(hue, 1.0f, 1.0f);
            guiGraphics.fill(hueX + i, hueY, hueX + i + 1, hueY + HUE_HEIGHT, color);
        }

        int hueSelectorX = hueX + (int) (hsv[0] * hueWidth) - 2;
        guiGraphics.fill(hueSelectorX, hueY - 2, hueSelectorX + 4, hueY + HUE_HEIGHT + 2, 0xFFFFFFFF);

        // Alpha slider
        int alphaX = pickerX;
        int alphaY = hueY + HUE_HEIGHT + 5;
        int alphaWidth = PICKER_WIDTH;
        int alpha = colorValue.getAlpha();

        // Checkerboard background for alpha
        for (int i = 0; i < alphaWidth; i += 8) {
            for (int j = 0; j < ALPHA_HEIGHT; j += 8) {
                if ((i / 8 + j / 8) % 2 == 0) {
                    guiGraphics.fill(alphaX + i, alphaY + j, alphaX + i + 8, alphaY + j + 8, 0xFFCCCCCC);
                } else {
                    guiGraphics.fill(alphaX + i, alphaY + j, alphaX + i + 8, alphaY + j + 8, 0xFFFFFFFF);
                }
            }
        }

        int colorNoAlpha = colorValue.getValue() & 0x00FFFFFF;
        for (int i = 0; i < alphaWidth; i++) {
            float a = (float) i / alphaWidth;
            int color = ARGB.color((int) (a * 255), colorNoAlpha);
            guiGraphics.fill(alphaX + i, alphaY, alphaX + i + 1, alphaY + ALPHA_HEIGHT, color);
        }

        int alphaSelectorX = alphaX + (int) (alpha / 255.0f * alphaWidth) - 2;
        guiGraphics.fill(alphaSelectorX, alphaY - 2, alphaSelectorX + 4, alphaY + ALPHA_HEIGHT + 2, 0xFFFFFFFF);

        // Rainbow speed slider
        int speedX = pickerX;
        int speedY = alphaY + ALPHA_HEIGHT + 5;
        int speedWidth = PICKER_WIDTH;

        guiGraphics.fill(speedX, speedY, speedX + speedWidth, speedY + SPEED_HEIGHT, 0xFF333333);
        int speedFill = (int) (colorValue.getRainbowSpeed() / 5.0f * speedWidth);
        guiGraphics.fill(speedX, speedY, speedX + speedFill, speedY + SPEED_HEIGHT, 0xFF00AAFF);
        int speedSelectorX = speedX + speedFill - 2;
        guiGraphics.fill(speedSelectorX, speedY - 2, speedSelectorX + 4, speedY + SPEED_HEIGHT + 2, 0xFFFFFFFF);

        // Labels
        renderLabel(guiGraphics, "Hue", hueX, hueY + HUE_HEIGHT + 2, 0xAAAAAA);
        renderLabel(guiGraphics, "Alpha: " + alpha, alphaX, alphaY + ALPHA_HEIGHT + 2, 0xAAAAAA);
        renderLabel(guiGraphics, "Rainbow Speed: " + String.format("%.1f", colorValue.getRainbowSpeed()), speedX, speedY + SPEED_HEIGHT + 2, 0xAAAAAA);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        // Check rainbow toggle
        int previewX = x + width - 55;
        int previewY = y + (height - 16) / 2;
        int toggleX = previewX - 55;
        int toggleY = previewY;
        int toggleWidth = 30;

        if (mouseX >= toggleX && mouseX <= toggleX + toggleWidth &&
            mouseY >= toggleY && mouseY <= toggleY + 16) {
            colorValue.setRainbow(!colorValue.isRainbow());
            return true;
        }

        // Check color preview click to toggle picker
        if (mouseX >= previewX && mouseX <= previewX + 16 &&
            mouseY >= previewY && mouseY <= previewY + 16) {
            showPicker = !showPicker;
            this.height = showPicker ? 120 + PICKER_HEIGHT + 20 : 20;
            return true;
        }

        if (showPicker) {
            int pickerX = x + 5;
            int pickerY = y + height - PICKER_HEIGHT - 25;

            // SV square
            int svX = pickerX;
            int svY = pickerY;
            int svSize = PICKER_WIDTH;
            if (mouseX >= svX && mouseX <= svX + svSize &&
                mouseY >= svY && mouseY <= svY + svSize) {
                draggingSatVal = true;
                updateFromSatVal(mouseX - svX, mouseY - svY);
                return true;
            }

            // Hue slider
            int hueX = pickerX;
            int hueY = svY + svSize + 5;
            int hueWidth = PICKER_WIDTH;
            if (mouseX >= hueX && mouseX <= hueX + hueWidth &&
                mouseY >= hueY && mouseY <= hueY + HUE_HEIGHT) {
                draggingHue = true;
                updateFromHue(mouseX - hueX, hueWidth);
                return true;
            }

            // Alpha slider
            int alphaX = pickerX;
            int alphaY = hueY + HUE_HEIGHT + 5;
            int alphaWidth = PICKER_WIDTH;
            if (mouseX >= alphaX && mouseX <= alphaX + alphaWidth &&
                mouseY >= alphaY && mouseY <= alphaY + ALPHA_HEIGHT) {
                draggingAlpha = true;
                updateFromAlpha(mouseX - alphaX, alphaWidth);
                return true;
            }

            // Rainbow speed slider
            int speedX = pickerX;
            int speedY = alphaY + ALPHA_HEIGHT + 5;
            int speedWidth = PICKER_WIDTH;
            if (mouseX >= speedX && mouseX <= speedX + speedWidth &&
                mouseY >= speedY && mouseY <= speedY + SPEED_HEIGHT) {
                draggingRainbowSpeed = true;
                updateFromSpeed(mouseX - speedX, speedWidth);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingHue = false;
        draggingSatVal = false;
        draggingAlpha = false;
        draggingRainbowSpeed = false;
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0) return false;

        if (showPicker) {
            int pickerX = x + 5;
            int pickerY = y + height - PICKER_HEIGHT - 25;
            int svX = pickerX;
            int svY = pickerY;
            int svSize = PICKER_WIDTH;
            int hueX = pickerX;
            int hueY = svY + svSize + 5;
            int hueWidth = PICKER_WIDTH;
            int alphaX = pickerX;
            int alphaY = hueY + HUE_HEIGHT + 5;
            int alphaWidth = PICKER_WIDTH;
            int speedX = pickerX;
            int speedY = alphaY + ALPHA_HEIGHT + 5;
            int speedWidth = PICKER_WIDTH;

            if (draggingSatVal) {
                updateFromSatVal(mouseX - svX, mouseY - svY);
                return true;
            }
            if (draggingHue) {
                updateFromHue(mouseX - hueX, hueWidth);
                return true;
            }
            if (draggingAlpha) {
                updateFromAlpha(mouseX - alphaX, alphaWidth);
                return true;
            }
            if (draggingRainbowSpeed) {
                updateFromSpeed(mouseX - speedX, speedWidth);
                return true;
            }
        }
        return false;
    }

    private void updateFromSatVal(double relX, double relY) {
        float s = Mth.clamp((float) (relX / PICKER_WIDTH), 0.0f, 1.0f);
        float v = Mth.clamp(1.0f - (float) (relY / PICKER_WIDTH), 0.0f, 1.0f);
        float[] hsv = colorValue.getHSV();
        colorValue.setHSV(hsv[0], s, v);
    }

    private void updateFromHue(double relX, int width) {
        float h = Mth.clamp((float) (relX / width), 0.0f, 1.0f);
        float[] hsv = colorValue.getHSV();
        colorValue.setHSV(h, hsv[1], hsv[2]);
    }

    private void updateFromAlpha(double relX, int width) {
        int alpha = Mth.clamp((int) (relX / width * 255), 0, 255);
        colorValue.setAlpha(alpha);
    }

    private void updateFromSpeed(double relX, int width) {
        float speed = Mth.clamp((float) (relX / width * 5.0f), 0.1f, 5.0f);
        colorValue.setRainbowSpeed(speed);
    }
}