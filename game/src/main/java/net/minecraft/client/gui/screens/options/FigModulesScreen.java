package net.minecraft.client.gui.screens.options;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class FigModulesScreen extends Screen {
   private static final Component TITLE = Component.literal("FIG CLIENT").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
   private static final int[] TAB_WIDTHS = {48, 76, 66, 68, 70, 66};
   private final Screen previousScreen;
   private final Options options;
   private final List<ModuleEntry> modules = new ArrayList<>();
   private final List<Button> moduleButtons = new ArrayList<>();
   private final List<Button> categoryButtons = new ArrayList<>();
   private final List<Button> toolButtons = new ArrayList<>();
   private Category category = Category.ALL;
   private String filter = "";
   private EditBox searchBox;
   private Button totemHealthButton;
   private int visibleModules;

   private enum Category {
      ALL("ALL"),
      VISUALS("VISUALS"),
      COMBAT("COMBAT"),
      QOL("QOL"),
      WORLD("WORLD"),
      TOOLS("TOOLS");

      private final String label;

      Category(final String label) {
         this.label = label;
      }
   }

   private record ModuleEntry(String name, Category category, OptionInstance<Boolean> option) {
   }

   public FigModulesScreen(final Screen lastScreen, final Options options) {
      super(TITLE);
      this.previousScreen = lastScreen;
      this.options = options;
      this.modules.add(new ModuleEntry("Fullbright", Category.VISUALS, options.figClientFullbright()));
      this.modules.add(new ModuleEntry("Zoom", Category.VISUALS, options.figClientZoom()));
      this.modules.add(new ModuleEntry("Compass and Coordinates", Category.VISUALS, options.figClientCompass()));
      this.modules.add(new ModuleEntry("Equipment HUD", Category.VISUALS, options.figClientEquipmentHud()));
      this.modules.add(new ModuleEntry("Inventory Alerts", Category.QOL, options.figClientInventoryAlerts()));
      this.modules.add(new ModuleEntry("Auto Refill (Singleplayer)", Category.QOL, options.figClientAutoRefill()));
      this.modules.add(new ModuleEntry("Auto Totem", Category.COMBAT, options.figClientAutoTotem()));
      this.modules.add(new ModuleEntry("Chunk Saver", Category.WORLD, options.figClientChunkSaver()));
      this.modules.add(new ModuleEntry("Entity Culling", Category.WORLD, options.figClientEntityCulling()));
      this.modules.add(new ModuleEntry("X-Ray", Category.WORLD, options.figClientXray()));
   }

   @Override
   protected void init() {
      int centerX = this.width / 2;
      this.searchBox = new EditBox(this.font, centerX - 145, 42, 290, 20, Component.literal("Search modules"));
      this.searchBox.setMaxLength(64);
      this.searchBox.setValue(this.filter);
      this.searchBox.setResponder(value -> {
         this.filter = value;
         this.updateVisibleModules();
      });
      this.addRenderableWidget(this.searchBox);

      int tabX = centerX - 216;
      Category[] categories = Category.values();
      for (int i = 0; i < categories.length; i++) {
         Category selected = categories[i];
         int width = TAB_WIDTHS[i];
         Button tabButton = this.addRenderableWidget(
            Button.builder(Component.literal(selected.label), button -> {
               this.category = selected;
               this.updateVisibleModules();
            }).bounds(tabX, 70, width, 20).build()
         );
         this.categoryButtons.add(tabButton);
         tabX += width + 4;
      }

      for (ModuleEntry module : this.modules) {
         Button button = this.addRenderableWidget(
            Button.builder(this.moduleLabel(module), pressed -> {
               module.option().set(!module.option().get());
               this.refreshModuleLabels();
               this.updateVisibleModules();
            }).bounds(0, 0, 190, 22).build()
         );
         this.moduleButtons.add(button);
      }

      this.totemHealthButton = this.addRenderableWidget(
         Button.builder(this.totemHealthLabel(), button -> {
            int health = this.options.figClientAutoTotemHealth().get();
            this.options.figClientAutoTotemHealth().set(health >= 20 ? 2 : health + 2);
            button.setMessage(this.totemHealthLabel());
            this.options.save();
         }).bounds(centerX - 95, 0, 190, 22).build()
      );
      this.toolButtons.add(this.addRenderableWidget(
         Button.builder(Component.literal("Waypoint Manager"), button -> Minecraft.getInstance().gui.setScreen(new FigWaypointsScreen(this, this.options)))
            .bounds(centerX - 200, 108, 190, 22)
            .build()
      ));
      this.toolButtons.add(this.addRenderableWidget(
         Button.builder(Component.literal("Block Search"), button -> Minecraft.getInstance().gui.setScreen(new FigBlockSearch(this, this.options)))
            .bounds(centerX + 10, 108, 190, 22)
            .build()
      ));
      this.toolButtons.add(this.addRenderableWidget(
         Button.builder(Component.literal("Edit HUD Layout"), button -> Minecraft.getInstance().gui.setScreen(new FigHudLayoutScreen(this, this.options)))
            .bounds(centerX - 95, 137, 190, 22)
            .build()
      ));
      this.addRenderableWidget(
         Button.builder(Component.literal("Done"), button -> this.onClose())
            .bounds(centerX - 45, this.height - 31, 90, 20)
            .build()
      );
      this.updateVisibleModules();
   }

   private Component moduleLabel(final ModuleEntry module) {
      return Component.literal(module.name() + (module.option().get() ? "  ON" : "  OFF"))
         .withStyle(module.option().get() ? ChatFormatting.AQUA : ChatFormatting.GRAY);
   }

   private Component totemHealthLabel() {
      return Component.literal("Totem trigger: " + this.options.figClientAutoTotemHealth().get() / 2.0 + " hearts");
   }

   private void refreshModuleLabels() {
      for (int i = 0; i < this.modules.size(); i++) {
         this.moduleButtons.get(i).setMessage(this.moduleLabel(this.modules.get(i)));
      }
      this.totemHealthButton.setMessage(this.totemHealthLabel());
   }

   private void updateVisibleModules() {
      if (this.moduleButtons.size() != this.modules.size()) {
         return;
      }
      String query = this.filter.trim().toLowerCase(Locale.ROOT);
      int centerX = this.width / 2;
      int row = 0;
      for (int i = 0; i < this.categoryButtons.size(); i++) {
         Category tab = Category.values()[i];
         this.categoryButtons.get(i).setMessage(Component.literal(tab == this.category ? "[" + tab.label + "]" : tab.label));
      }
      for (int i = 0; i < this.modules.size(); i++) {
         ModuleEntry module = this.modules.get(i);
         Button button = this.moduleButtons.get(i);
         boolean visible = (query.isEmpty() ? this.category == Category.ALL || module.category() == this.category
            : module.name().toLowerCase(Locale.ROOT).contains(query));
         button.visible = visible;
         if (visible) {
            int column = row % 2;
            int rowY = row / 2;
            button.setPosition(centerX - 200 + column * 210, 108 + rowY * 28);
            row++;
         }
      }
      this.visibleModules = row;
      this.totemHealthButton.visible = query.isEmpty()
         && this.category == Category.COMBAT
         && this.options.figClientAutoTotem().get();
      if (this.totemHealthButton.visible) {
         this.totemHealthButton.setPosition(centerX - 95, 108 + (row / 2 + 1) * 28);
      }
      for (Button button : this.toolButtons) {
         button.visible = this.category == Category.TOOLS && query.isEmpty();
      }
   }

   @Override
   public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      graphics.fill(0, 0, this.width, this.height, 0xC91B3443);
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      graphics.fill(this.width / 2 - 216, 31, this.width / 2 + 216, 33, 0xFF7CD8F3);
      graphics.centeredText(this.font, TITLE, this.width / 2, 19, 0xFFFFFFFF);
      graphics.centeredText(this.font, "Press Right Shift to return here in-game. Rebind module toggles in Controls.", this.width / 2, this.height - 45, 0xFFB9D8E8);
      if (this.visibleModules == 0 && !this.filter.isEmpty()) {
         graphics.centeredText(this.font, "No modules match \"" + this.filter + "\".", this.width / 2, 122, 0xFFB9D8E8);
      }
   }

   @Override
   public void onClose() {
      this.minecraft.gui.setScreen(this.previousScreen);
   }
}
