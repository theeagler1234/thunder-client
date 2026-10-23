package net.minecraft.client.gui.screens.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.module.ModuleManager;
import net.minecraft.client.value.StringValue;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ClickGUIScreen extends Screen {
    private static final int CATEGORY_WIDTH = 120;
    private static final int CATEGORY_HEIGHT = 25;
    private static final int MODULE_HEIGHT = 22;
    private static final int SETTING_HEIGHT = 20;
    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_PADDING = 4;

    private final List<CategoryPanel> categoryPanels = new ArrayList<>();
    private CategoryPanel draggedPanel = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private long lastClickTime = 0;
    private CategoryPanel lastClickedPanel = null;

    public ClickGUIScreen() {
        super(Component.literal("ClickGUI"));
        initializePanels();
    }

    private void initializePanels() {
        categoryPanels.clear();
        int x = 10;
        int y = 10;

        for (ModuleCategory category : ModuleCategory.values()) {
            List<Module> modules = ModuleManager.getInstance().getModulesByCategory(category);
            if (!modules.isEmpty()) {
                CategoryPanel panel = new CategoryPanel(category, x, y, PANEL_WIDTH);
                categoryPanels.add(panel);
                x += CATEGORY_WIDTH + 10;
                if (x > width - PANEL_WIDTH - 10) {
                    x = 10;
                    y += 300;
                }
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        initializePanels();

        // Add close button
        this.addRenderableWidget(Button.builder(Component.literal("Close"), button -> {
            this.onClose();
        }).bounds(this.width - 60, 5, 55, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render semi-transparent background
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0x80000000, 0x80000000);

        // Render title
        guiGraphics.drawString(this.font, "ClickGUI - Eaglercraft", 5, 5, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "Right-click module for settings | Drag category headers to move", 5, 18, 0xAAAAAA, false);

        // Render category panels
        for (CategoryPanel panel : categoryPanels) {
            panel.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check panels in reverse order (topmost first)
        for (int i = categoryPanels.size() - 1; i >= 0; i--) {
            CategoryPanel panel = categoryPanels.get(i);
            if (panel.mouseClicked(mouseX, mouseY, button)) {
                // Bring to front
                categoryPanels.remove(i);
                categoryPanels.add(panel);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggedPanel != null) {
            draggedPanel = null;
            return true;
        }
        for (CategoryPanel panel : categoryPanels) {
            if (panel.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggedPanel != null && button == 0) {
            draggedPanel.setPosition(
                (int) (mouseX - dragOffsetX),
                (int) (mouseY - dragOffsetY)
            );
            return true;
        }
        for (CategoryPanel panel : categoryPanels) {
            if (panel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (CategoryPanel panel : categoryPanels) {
            if (panel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            this.onClose();
            return true;
        }

        // Forward key events to category panels for setting entries
        for (CategoryPanel panel : categoryPanels) {
            if (panel.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // Forward character events to category panels for setting entries
        for (CategoryPanel panel : categoryPanels) {
            if (panel.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void setDraggedPanel(CategoryPanel panel, int offsetX, int offsetY) {
        this.draggedPanel = panel;
        this.dragOffsetX = offsetX;
        this.dragOffsetY = offsetY;
    }

    public List<CategoryPanel> getCategoryPanels() {
        return categoryPanels;
    }
}