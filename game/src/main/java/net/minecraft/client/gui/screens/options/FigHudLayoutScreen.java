package net.minecraft.client.gui.screens.options;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class FigHudLayoutScreen extends Screen {
   private static final Component TITLE = Component.literal("HUD Layout").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
   private final Screen previousScreen;
   private final Options options;
   private int compassX;
   private int compassY;
   private int equipmentX;
   private int equipmentY;
   private int hotbarX;
   private int hotbarY;
   private int dragging = -1;

   public FigHudLayoutScreen(final Screen previousScreen, final Options options) {
      super(TITLE);
      this.previousScreen = previousScreen;
      this.options = options;
   }

   @Override
   protected void init() {
      this.compassX = this.options.figClientCompassX().get();
      this.compassY = this.options.figClientCompassY().get();
      this.equipmentX = this.options.figClientEquipmentHudX().get();
      this.equipmentY = this.options.figClientEquipmentHudY().get();
      this.hotbarX = this.options.figClientHotbarX().get();
      this.hotbarY = this.options.figClientHotbarY().get();
      this.addRenderableWidget(
         Button.builder(Component.literal("Reset Positions"), button -> {
            this.compassX = this.compassY = this.equipmentX = this.equipmentY = this.hotbarX = this.hotbarY = 0;
            this.savePositions();
         }).bounds(this.width / 2 - 105, 24, 100, 20).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Done"), button -> this.onClose())
            .bounds(this.width / 2 + 5, 24, 100, 20)
            .build()
      );
   }

   @Override
   public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
      if (event.button() == 0) {
         int mouseX = (int)event.x();
         int mouseY = (int)event.y();
         if (this.contains(mouseX, mouseY, this.compassBounds())) {
            this.dragging = 0;
            return true;
         }
         if (this.contains(mouseX, mouseY, this.equipmentBounds())) {
            this.dragging = 1;
            return true;
         }
         if (this.contains(mouseX, mouseY, this.hotbarBounds())) {
            this.dragging = 2;
            return true;
         }
      }
      return super.mouseClicked(event, doubleClick);
   }

   @Override
   public boolean mouseDragged(final MouseButtonEvent event, final double dx, final double dy) {
      if (this.dragging < 0 || event.button() != 0) {
         return super.mouseDragged(event, dx, dy);
      }

      int moveX = (int)Math.round(dx);
      int moveY = (int)Math.round(dy);
      switch (this.dragging) {
         case 0 -> {
            this.compassX = clamp(this.compassX + moveX);
            this.compassY = clamp(this.compassY + moveY);
         }
         case 1 -> {
            this.equipmentX = clamp(this.equipmentX + moveX);
            this.equipmentY = clamp(this.equipmentY + moveY);
         }
         case 2 -> {
            this.hotbarX = clamp(this.hotbarX + moveX);
            this.hotbarY = clamp(this.hotbarY + moveY);
         }
      }
      return true;
   }

   @Override
   public boolean mouseReleased(final MouseButtonEvent event) {
      if (this.dragging >= 0) {
         this.dragging = -1;
         this.savePositions();
         return true;
      }
      return super.mouseReleased(event);
   }

   private static int clamp(final int value) {
      return Math.clamp(value, -4096, 4096);
   }

   private boolean contains(final int x, final int y, final int[] bounds) {
      return x >= bounds[0] && x <= bounds[2] && y >= bounds[1] && y <= bounds[3];
   }

   private int[] compassBounds() {
      int centerX = this.width / 2 + this.compassX;
      int centerY = 38 + this.compassY;
      return new int[] {centerX - 36, centerY - 17, centerX + 36, centerY + 43};
   }

   private int[] equipmentBounds() {
      int left = this.width / 2 - 53 + this.equipmentX;
      int top = this.height - 81 + this.equipmentY;
      return new int[] {left, top, left + 106, top + 26};
   }

   private int[] hotbarBounds() {
      int left = this.width / 2 - 91 + this.hotbarX;
      int top = this.height - 22 + this.hotbarY;
      return new int[] {left, top, left + 182, top + 22};
   }

   private void savePositions() {
      this.options.figClientCompassX().set(this.compassX);
      this.options.figClientCompassY().set(this.compassY);
      this.options.figClientEquipmentHudX().set(this.equipmentX);
      this.options.figClientEquipmentHudY().set(this.equipmentY);
      this.options.figClientHotbarX().set(this.hotbarX);
      this.options.figClientHotbarY().set(this.hotbarY);
      this.options.save();
   }

   @Override
   public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      graphics.fill(0, 0, this.width, this.height, 0xB51B3443);
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      graphics.centeredText(this.font, TITLE, this.width / 2, 58, 0xFFFFFFFF);
      graphics.centeredText(this.font, "Drag a panel to move it. Positions save when you release the mouse.", this.width / 2, 72, 0xFFB9D8E8);
      this.drawPanel(graphics, this.compassBounds(), "COMPASS / COORDINATES", 0);
      this.drawPanel(graphics, this.equipmentBounds(), "EQUIPMENT HUD", 1);
      this.drawPanel(graphics, this.hotbarBounds(), "HOTBAR", 2);
      graphics.centeredText(this.font, "Preview only - enable each HUD module in Fig Mods.", this.width / 2, this.height - 14, 0xFFB9D8E8);
   }

   private void drawPanel(final GuiGraphicsExtractor graphics, final int[] bounds, final String label, final int id) {
      int color = this.dragging == id ? 0xD050BCE7 : 0xB5265368;
      graphics.fill(bounds[0], bounds[1], bounds[2], bounds[3], color);
      graphics.outline(bounds[0], bounds[1], bounds[2] - bounds[0], bounds[3] - bounds[1], 0xFF7CD8F3);
      graphics.centeredText(this.font, label, (bounds[0] + bounds[2]) / 2, bounds[1] + 4, 0xFFFFFFFF);
   }

   @Override
   public void onClose() {
      this.savePositions();
      this.minecraft.gui.setScreen(this.previousScreen);
   }
}
