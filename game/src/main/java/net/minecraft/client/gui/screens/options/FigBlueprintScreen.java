package net.minecraft.client.gui.screens.options;

import net.lax1dude.eaglercraft.v1_8.EagRuntime;
import net.lax1dude.eaglercraft.v1_8.internal.FileChooserResult;
import net.minecraft.client.FigBlueprintManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class FigBlueprintScreen extends Screen {
   private final Screen previousScreen;
   private boolean waitingForFile;
   private Component status = Component.literal("No blueprint loaded");

   public FigBlueprintScreen(final Screen previousScreen) {
      super(Component.literal("Fig Blueprint Guide"));
      this.previousScreen = previousScreen;
   }

   @Override
   protected void init() {
      this.addRenderableWidget(
         Button.builder(Component.literal("Import vanilla .nbt"), button -> {
            if (this.minecraft.isLocalServer() && !this.minecraft.isMultiplayerServer()) {
               this.waitingForFile = true;
               EagRuntime.displayFileChooser(null, "nbt");
            } else {
               this.status = Component.literal("Blueprint guides are available in private singleplayer worlds");
            }
         }).bounds(this.width / 2 - 100, this.height / 2 - 20, 200, 20).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Clear blueprint"), button -> {
            FigBlueprintManager.clear();
            this.status = Component.literal("Blueprint cleared");
         }).bounds(this.width / 2 - 100, this.height / 2 + 5, 200, 20).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Done"), button -> this.minecraft.gui.setScreen(this.previousScreen))
            .bounds(this.width / 2 - 100, this.height / 2 + 35, 200, 20)
            .build()
      );
   }

   @Override
   public void tick() {
      if (this.waitingForFile && EagRuntime.fileChooserHasResult()) {
         this.waitingForFile = false;
         FileChooserResult result = EagRuntime.getFileChooserResult();
         EagRuntime.clearFileChooserResult();
         if (result != null && this.minecraft.level != null && this.minecraft.player != null) {
            try {
               FigBlueprintManager.load(result.fileData, this.minecraft.level, this.minecraft.player.blockPosition());
               this.status = Component.literal("Loaded " + result.fileName + " at your current position");
            } catch (Exception exception) {
               this.status = Component.literal("Could not load blueprint: " + exception.getMessage());
            }
         }
      }
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      graphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 65, -1);
      graphics.centeredText(this.font, this.status, this.width / 2, this.height / 2 - 42, -1);
   }

   @Override
   public void onClose() {
      this.minecraft.gui.setScreen(this.previousScreen);
   }
}