package net.minecraft.client.module.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.ColorValue;
import net.minecraft.client.InputConstants;

public class KeystrokesModule extends Module {
    private final BooleanValue w = registerValue(new BooleanValue("W", "Show W", true));
    private final BooleanValue a = registerValue(new BooleanValue("A", "Show A", true));
    private final BooleanValue s = registerValue(new BooleanValue("S", "Show S", true));
    private final BooleanValue d = registerValue(new BooleanValue("D", "Show D", true));
    private final BooleanValue space = registerValue(new BooleanValue("Space", "Show Space", true));
    private final BooleanValue shift = registerValue(new BooleanValue("Shift", "Show Shift", true));
    private final BooleanValue leftClick = registerValue(new BooleanValue("LeftClick", "Show Left Click", true));
    private final BooleanValue rightClick = registerValue(new BooleanValue("RightClick", "Show Right Click", true));
    private final ColorValue pressedColor = registerValue(new ColorValue("PressedColor", "Pressed key color", 0xFF00FF00));
    private final ColorValue releasedColor = registerValue(new ColorValue("ReleasedColor", "Released key color", 0xFFFFFFFF));
    private final ColorValue backgroundColor = registerValue(new ColorValue("BackgroundColor", "Background color", 0x80000000));

    private int x = 10;
    private int y = 100;
    private final int keySize = 30;
    private final int keyGap = 5;

    public KeystrokesModule() {
        super("Keystrokes", "Shows keystroke display", ModuleCategory.MISC);
    }

    @Override
    public void onRender(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();

        renderKey(guiGraphics, "W", w.getValue(), mc.options.keyUp.isDown(), x + keySize + keyGap, y - keySize - keyGap);
        renderKey(guiGraphics, "A", a.getValue(), mc.options.keyLeft.isDown(), x, y);
        renderKey(guiGraphics, "S", s.getValue(), mc.options.keyDown.isDown(), x + keySize + keyGap, y);
        renderKey(guiGraphics, "D", d.getValue(), mc.options.keyRight.isDown(), x + 2 * (keySize + keyGap), y);
        renderKey(guiGraphics, "Space", space.getValue(), mc.options.keyJump.isDown(), x + keySize + keyGap, y + keySize + keyGap);
        renderKey(guiGraphics, "Shift", shift.getValue(), mc.options.keyShift.isDown(), x, y + keySize + keyGap);

        if (leftClick.getValue() || rightClick.getValue()) {
            int mouseY = y + 2 * (keySize + keyGap);
            if (leftClick.getValue()) {
                renderKey(guiGraphics, "LMB", true, mc.mouseHandler.isLeftPressed(), x, mouseY);
            }
            if (rightClick.getValue()) {
                int offset = leftClick.getValue() ? keySize + keyGap : 0;
                renderKey(guiGraphics, "RMB", true, mc.mouseHandler.isRightPressed(), x + offset, mouseY);
            }
        }
    }

    private void renderKey(GuiGraphics guiGraphics, String label, boolean enabled, boolean pressed, int x, int y) {
        if (!enabled) return;

        int color = pressed ? pressedColor.getColor() : releasedColor.getColor();
        int bgColor = backgroundColor.getColor();

        guiGraphics.fill(x, y, x + keySize, y + keySize, bgColor);
        guiGraphics.fill(x, y, x + keySize, y + 1, color);
        guiGraphics.fill(x, y + keySize - 1, x + keySize, y + keySize, color);
        guiGraphics.fill(x, y, x + 1, y + keySize, color);
        guiGraphics.fill(x + keySize - 1, y, x + keySize, y + keySize, color);

        int textWidth = Minecraft.getInstance().font.width(label);
        int textX = x + (keySize - textWidth) / 2;
        int textY = y + (keySize - 8) / 2;
        guiGraphics.drawString(Minecraft.getInstance().font, label, textX, textY, color, true);
    }
}