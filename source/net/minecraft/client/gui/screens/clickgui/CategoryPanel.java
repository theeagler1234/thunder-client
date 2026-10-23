package net.minecraft.client.gui.screens.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.module.ModuleManager;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.ColorValue;
import net.minecraft.client.value.EnumValue;
import net.minecraft.client.value.FloatValue;
import net.minecraft.client.value.StringValue;
import net.minecraft.client.value.Value;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CategoryPanel extends AbstractWidget {
    private final ModuleCategory category;
    private final List<ModuleEntry> moduleEntries = new ArrayList<>();
    private final List<SettingEntry> settingEntries = new ArrayList<>();
    private boolean expanded = true;
    private boolean settingsOpen = false;
    private Module selectedModule = null;
    private double scrollAmount = 0;
    private double targetScrollAmount = 0;
    private int contentHeight = 0;
    private boolean draggingScrollbar = false;
    private int scrollbarX;
    private int scrollbarWidth = 6;

    public CategoryPanel(ModuleCategory category, int x, int y, int width) {
        super(x, y, width, 0, Component.literal(category.getName()));
        this.category = category;
        rebuildEntries();
        calculateHeight();
    }

    private void rebuildEntries() {
        moduleEntries.clear();
        settingEntries.clear();
        selectedModule = null;
        settingsOpen = false;

        List<Module> modules = ModuleManager.getInstance().getModulesByCategory(category);
        int y = CATEGORY_HEIGHT + 4;

        for (Module module : modules) {
            if (module.isVisible()) {
                ModuleEntry entry = new ModuleEntry(module, this.getX() + 2, this.getY() + y, this.getWidth() - 4, MODULE_HEIGHT);
                moduleEntries.add(entry);
                y += MODULE_HEIGHT + 2;
            }
        }
    }

    private void calculateHeight() {
        int height = CATEGORY_HEIGHT + 4;
        if (expanded) {
            for (ModuleEntry entry : moduleEntries) {
                height += entry.getHeight() + 2;
            }
            if (settingsOpen && selectedModule != null) {
                for (SettingEntry entry : settingEntries) {
                    height += entry.getHeight() + 2;
                }
            }
        }
        this.setHeight(Math.min(height, 400));
        this.contentHeight = height;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = this.getX();
        int y = this.getY();
        int width = this.getWidth();
        int height = this.getHeight();

        // Background
        int bgColor = 0xE01A1A1A;
        int headerColor = category.getColor() | 0xE0000000;
        int borderColor = 0xFF333333;

        // Panel background
        guiGraphics.fill(x, y, x + width, y + height, bgColor);
        guiGraphics.fill(x, y, x + width, y + height, 0x20FFFFFF);

        // Header
        guiGraphics.fill(x, y, x + width, y + CATEGORY_HEIGHT, headerColor);
        guiGraphics.fill(x, y + CATEGORY_HEIGHT - 1, x + width, y + CATEGORY_HEIGHT, borderColor);

        // Category icon and name
        String headerText = category.getIcon() + " " + category.getName();
        int textWidth = Minecraft.getInstance().font.width(headerText);
        guiGraphics.drawString(Minecraft.getInstance().font, headerText,
            x + (width - textWidth) / 2, y + (CATEGORY_HEIGHT - 8) / 2, 0xFFFFFF, false);

        // Expand/collapse indicator
        String indicator = expanded ? "▼" : "▶";
        int indicatorWidth = Minecraft.getInstance().font.width(indicator);
        guiGraphics.drawString(Minecraft.getInstance().font, indicator,
            x + width - indicatorWidth - 5, y + (CATEGORY_HEIGHT - 8) / 2, 0xFFFFFF, false);

        // Module count
        int enabledCount = 0;
        for (ModuleEntry entry : moduleEntries) {
            if (entry.module.isEnabled()) enabledCount++;
        }
        String countText = enabledCount + "/" + moduleEntries.size();
        int countWidth = Minecraft.getInstance().font.width(countText);
        guiGraphics.drawString(Minecraft.getInstance().font, countText,
            x + 5, y + (CATEGORY_HEIGHT - 8) / 2, 0xAAAAAA, false);

        if (!expanded) {
            renderScrollbar(guiGraphics, mouseX, mouseY);
            return;
        }

        // Render modules
        int clipY = y + CATEGORY_HEIGHT;
        int clipHeight = height - CATEGORY_HEIGHT;

        guiGraphics.enableScissor(x, clipY, x + width, clipY + clipHeight);

        int renderY = (int) (clipY - scrollAmount);

        for (ModuleEntry entry : moduleEntries) {
            if (renderY + entry.getHeight() >= clipY && renderY <= clipY + clipHeight) {
                entry.setY(renderY);
                entry.render(guiGraphics, mouseX, mouseY, partialTick);
            }
            renderY += entry.getHeight() + 2;
        }

        // Render settings if open
        if (settingsOpen && selectedModule != null) {
            for (SettingEntry entry : settingEntries) {
                if (renderY + entry.getHeight() >= clipY && renderY <= clipY + clipHeight) {
                    entry.setY(renderY);
                    entry.render(guiGraphics, mouseX, mouseY, partialTick);
                }
                renderY += entry.getHeight() + 2;
            }
        }

        guiGraphics.disableScissor();

        renderScrollbar(guiGraphics, mouseX, mouseY);
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (contentHeight <= this.getHeight()) return;

        scrollbarX = this.getX() + this.getWidth() - scrollbarWidth - 2;
        int trackY = this.getY() + CATEGORY_HEIGHT;
        int trackHeight = this.getHeight() - CATEGORY_HEIGHT;

        // Track
        guiGraphics.fill(scrollbarX, trackY, scrollbarX + scrollbarWidth, trackY + trackHeight, 0x40000000);

        // Thumb
        double scrollRatio = scrollAmount / Math.max(1, contentHeight - this.getHeight());
        int thumbHeight = Math.max(20, (int) (trackHeight * ((double) this.getHeight() / contentHeight)));
        int thumbY = trackY + (int) ((trackHeight - thumbHeight) * scrollRatio);

        boolean hoverScrollbar = mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
                                 mouseY >= thumbY && mouseY <= thumbY + thumbHeight;

        guiGraphics.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight,
            hoverScrollbar || draggingScrollbar ? 0xFFAAAAAA : 0xFF888888);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isMouseOver(mouseX, mouseY)) return false;

        int x = this.getX();
        int y = this.getY();

        // Header click
        if (mouseY >= y && mouseY <= y + CATEGORY_HEIGHT) {
            if (button == 0) { // Left click - expand/collapse
                expanded = !expanded;
                calculateHeight();
            } else if (button == 1) { // Right click - drag
                ClickGUIScreen screen = (ClickGUIScreen) Minecraft.getInstance().gui.screen();
                if (screen != null) {
                    screen.setDraggedPanel(this, (int) (mouseX - x), (int) (mouseY - y));
                }
            }
            return true;
        }

        // Scrollbar drag
        if (contentHeight > this.getHeight() &&
            mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth) {
            draggingScrollbar = true;
            return true;
        }

        if (!expanded) return true;

        // Module clicks
        int clipY = y + CATEGORY_HEIGHT;
        int renderY = (int) (clipY - scrollAmount);

        for (ModuleEntry entry : moduleEntries) {
            entry.setY(renderY);
            if (entry.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            renderY += entry.getHeight() + 2;
        }

        // Settings clicks
        if (settingsOpen && selectedModule != null) {
            for (SettingEntry entry : settingEntries) {
                entry.setY(renderY);
                if (entry.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                renderY += entry.getHeight() + 2;
            }
        }

        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbar = false;

        if (!expanded) return false;

        int clipY = this.getY() + CATEGORY_HEIGHT;
        int renderY = (int) (clipY - scrollAmount);

        for (ModuleEntry entry : moduleEntries) {
            entry.setY(renderY);
            if (entry.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
            renderY += entry.getHeight() + 2;
        }

        if (settingsOpen && selectedModule != null) {
            for (SettingEntry entry : settingEntries) {
                entry.setY(renderY);
                if (entry.mouseReleased(mouseX, mouseY, button)) {
                    return true;
                }
                renderY += entry.getHeight() + 2;
            }
        }

        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar && button == 0) {
            int trackY = this.getY() + CATEGORY_HEIGHT;
            int trackHeight = this.getHeight() - CATEGORY_HEIGHT;
            int thumbHeight = Math.max(20, (int) (trackHeight * ((double) this.getHeight() / contentHeight)));
            double ratio = (mouseY - trackY - thumbHeight / 2.0) / (trackHeight - thumbHeight);
            scrollAmount = Mth.clamp(ratio * (contentHeight - this.getHeight()), 0, contentHeight - this.getHeight());
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.isMouseOver(mouseX, mouseY)) return false;

        if (contentHeight > this.getHeight()) {
            scrollAmount = Mth.clamp(scrollAmount - scrollY * 20, 0, Math.max(0, contentHeight - this.getHeight()));
            return true;
        }
        return false;
    }

    public void openSettings(Module module) {
        this.selectedModule = module;
        this.settingsOpen = true;
        this.settingEntries.clear();

        int y = 0;
        for (Value<?> value : module.getValues()) {
            SettingEntry entry;
            if (value instanceof BooleanValue) {
                entry = new BooleanSettingEntry((BooleanValue) value, this.getX() + 10, this.getY() + y, this.getWidth() - 20, SETTING_HEIGHT);
            } else if (value instanceof FloatValue) {
                entry = new FloatSettingEntry((FloatValue) value, this.getX() + 10, this.getY() + y, this.getWidth() - 20, SETTING_HEIGHT);
            } else if (value instanceof ColorValue) {
                entry = new ColorSettingEntry((ColorValue) value, this.getX() + 10, this.getY() + y, this.getWidth() - 20, SETTING_HEIGHT + 60);
            } else if (value instanceof EnumValue) {
                entry = new EnumSettingEntry((EnumValue<?>) value, this.getX() + 10, this.getY() + y, this.getWidth() - 20, SETTING_HEIGHT);
            } else if (value instanceof StringValue) {
                entry = new StringSettingEntry((StringValue) value, this.getX() + 10, this.getY() + y, this.getWidth() - 20, SETTING_HEIGHT);
            } else {
                entry = new GenericSettingEntry(value, this.getX() + 10, this.getY() + y, this.getWidth() - 20, SETTING_HEIGHT);
            }
            settingEntries.add(entry);
            y += entry.getHeight() + 4;
        }
        calculateHeight();
    }

    public void closeSettings() {
        this.settingsOpen = false;
        this.selectedModule = null;
        this.settingEntries.clear();
        calculateHeight();
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public boolean isSettingsOpen() {
        return settingsOpen;
    }

    public Module getSelectedModule() {
        return selectedModule;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (settingsOpen && selectedModule != null) {
            for (SettingEntry entry : settingEntries) {
                if (entry instanceof StringSettingEntry stringEntry) {
                    if (stringEntry.keyPressed(keyCode, scanCode, modifiers)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (settingsOpen && selectedModule != null) {
            for (SettingEntry entry : settingEntries) {
                if (entry instanceof StringSettingEntry stringEntry) {
                    if (stringEntry.charTyped(codePoint, modifiers)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}