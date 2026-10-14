package net.minecraft.client.gui.screens.options;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class FigWaypointsScreen extends Screen {
   private static final Component TITLE = Component.literal("Waypoint Manager").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
   private final Screen previousScreen;
   private final Options options;

   public FigWaypointsScreen(final Screen previousScreen, final Options options) {
      super(TITLE);
      this.previousScreen = previousScreen;
      this.options = options;
   }

   @Override
   protected void init() {
      int centerX = this.width / 2;
      int top = this.height / 2 + 12;
      this.addRenderableWidget(
         Button.builder(Component.literal("Clear Latest Death"), button -> this.clearWaypoint(this.options.figClientLatestDeath()))
            .bounds(centerX - 102, top, 100, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Clear Previous Death"), button -> this.clearWaypoint(this.options.figClientPreviousDeath()))
            .bounds(centerX + 2, top, 100, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Done"), button -> this.onClose())
            .bounds(centerX - 100, top + 28, 200, 20)
            .build()
      );
   }

   private void clearWaypoint(final net.minecraft.client.OptionInstance<String> waypoint) {
      waypoint.set("");
      this.options.save();
   }

   @Override
   public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractBackground(graphics, mouseX, mouseY, a);
      graphics.fill(0, 0, this.width, this.height, 0x26328CB8);
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      int centerX = this.width / 2;
      int centerY = this.height / 2;
      graphics.fill(centerX - 150, centerY - 82, centerX + 150, centerY - 80, 0xFF7CD8F3);
      graphics.centeredText(this.font, TITLE, centerX, centerY - 105, 0xFFFFFFFF);
      drawWaypoint(graphics, centerX, centerY - 58, "LATEST DEATH", this.options.figClientLatestDeath().get());
      drawWaypoint(graphics, centerX, centerY - 18, "PREVIOUS DEATH", this.options.figClientPreviousDeath().get());
   }

   private void drawWaypoint(final GuiGraphicsExtractor graphics, final int centerX, final int y, final String title, final String value) {
      graphics.text(this.font, Component.literal(title).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), centerX - 140, y, 0xFFFFFFFF);
      if (value.isEmpty()) {
         graphics.text(this.font, "No death recorded", centerX - 140, y + 13, 0xFFB9D8E8);
         return;
      }

      String[] parts = value.split("\\|", 4);
      if (parts.length != 4) {
         graphics.text(this.font, "Saved waypoint data is invalid", centerX - 140, y + 13, 0xFFFF8080);
         return;
      }

      graphics.text(this.font, "Dimension: " + parts[0] + "   X: " + parts[1] + "   Y: " + parts[2] + "   Z: " + parts[3],
         centerX - 140, y + 13, 0xFFFFFFFF);
   }

   @Override
   public void onClose() {
      this.minecraft.gui.setScreen(this.previousScreen);
   }
}
