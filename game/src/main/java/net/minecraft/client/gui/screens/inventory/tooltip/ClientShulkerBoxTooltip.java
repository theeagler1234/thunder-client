package net.minecraft.client.gui.screens.inventory.tooltip;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.tooltip.ShulkerBoxTooltip;

public class ClientShulkerBoxTooltip implements ClientTooltipComponent {
   private static final Identifier SLOT_BACKGROUND = Identifier.withDefaultNamespace("container/bundle/slot_background");
   private static final int COLUMNS = 9;
   private static final int ROWS = 3;
   private static final int SLOT_SIZE = 18;
   private final List<ItemStack> contents;

   public ClientShulkerBoxTooltip(final ShulkerBoxTooltip tooltip) {
      this.contents = tooltip.contents().allItemsCopyStream().limit(COLUMNS * ROWS).toList();
   }

   @Override
   public int getHeight(final Font font) {
      return ROWS * SLOT_SIZE + 4;
   }

   @Override
   public int getWidth(final Font font) {
      return COLUMNS * SLOT_SIZE + 4;
   }

   @Override
   public void extractImage(final Font font, final int x, final int y, final int width, final int height, final GuiGraphicsExtractor graphics) {
      for (int slot = 0; slot < COLUMNS * ROWS; slot++) {
         int slotX = x + 2 + slot % COLUMNS * SLOT_SIZE;
         int slotY = y + 2 + slot / COLUMNS * SLOT_SIZE;
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_BACKGROUND, slotX, slotY, SLOT_SIZE, SLOT_SIZE);
         if (slot < this.contents.size()) {
            ItemStack item = this.contents.get(slot);
            if (!item.isEmpty()) {
               graphics.item(item, slotX + 1, slotY + 1, slot);
               graphics.itemDecorations(font, item, slotX + 1, slotY + 1);
            }
         }
      }
   }
}