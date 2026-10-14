package net.minecraft.client.gui.screens.options;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.Nullable;

public class FigBlockSearch extends Screen {
   private static final int MAX_MATCHES = 64;
   private static final int SCAN_BUDGET_PER_FRAME = 2048;
   private static final int MAX_CHUNK_RADIUS = 4;
   private static final Component TITLE = Component.literal("Block Search").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
   private final Screen previousScreen;
   private final Options options;
   private EditBox searchBox;
   private static final List<BlockPos> MATCHES = new ArrayList<>();
   private static final List<LevelChunk> SCAN_CHUNKS = new ArrayList<>();
   private static final Set<Block> SEARCH_BLOCKS = new HashSet<>();
   private static @Nullable Object scannedLevel;
   private static String searchedText = "";
   private static int chunkIndex;
   private static int blockIndex;
   private static int minBuildHeight;
   private static int scanHeight;
   private static int scanCenterX;
   private static int scanCenterZ;
   private static boolean scanning;
   private static String status = "Enter a block name or ID, such as diamond_ore.";

   public FigBlockSearch(final Screen previousScreen, final Options options) {
      super(TITLE);
      this.previousScreen = previousScreen;
      this.options = options;
   }

   @Override
   protected void init() {
      int centerX = this.width / 2;
      int top = this.height / 2 - 28;
      this.searchBox = new EditBox(this.font, centerX - 150, top, 300, 20, Component.literal("Block name or ID"));
      this.searchBox.setMaxLength(128);
      this.searchBox.setValue(this.options.figClientBlockSearch().get());
      this.addRenderableWidget(this.searchBox);
      this.addRenderableWidget(
         Button.builder(Component.literal("Search Loaded Chunks"), button -> this.beginSearch())
            .bounds(centerX - 150, top + 30, 148, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Clear"), button -> clearSearch())
            .bounds(centerX + 2, top + 30, 70, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Done"), button -> this.onClose())
            .bounds(centerX - 35, top + 60, 70, 20)
            .build()
      );
   }

   private void beginSearch() {
      String query = this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
      this.options.figClientBlockSearch().set(query);
      this.options.save();
      String error = startSearch(this.minecraft, query);
      if (error != null) {
         status = error;
      }
   }

   private static @Nullable String startSearch(final Minecraft minecraft, final String query) {
      MATCHES.clear();
      SCAN_CHUNKS.clear();
      SEARCH_BLOCKS.clear();
      scannedLevel = null;
      scanning = false;
      searchedText = query;
      if (query.isBlank()) {
         return "Enter a block name or ID to search.";
      }

      String normalizedQuery = query.startsWith("minecraft:") ? query.substring("minecraft:".length()) : query;
      if (normalizedQuery.isBlank()) {
         return "Enter a block name after the namespace.";
      }
      for (Block block : BuiltInRegistries.BLOCK) {
         if (BuiltInRegistries.BLOCK.getKey(block).toString().toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
            SEARCH_BLOCKS.add(block);
         }
      }

      if (SEARCH_BLOCKS.isEmpty()) {
         status = "No block IDs match \"" + query + "\".";
         return status;
      }
      if (minecraft.level == null || minecraft.player == null) {
         status = "Join a world before searching.";
         return status;
      }

      prepareScan(minecraft);
      return null;
   }

   private static void prepareScan(final Minecraft minecraft) {
      if (minecraft.level == null || minecraft.player == null) {
         scanning = false;
         return;
      }

      var level = minecraft.level;
      int playerChunkX = minecraft.player.chunkPosition().x();
      int playerChunkZ = minecraft.player.chunkPosition().z();
      scanCenterX = playerChunkX;
      scanCenterZ = playerChunkZ;
      minBuildHeight = level.getMinY();
      scanHeight = level.getHeight();
      int radius = Math.min(MAX_CHUNK_RADIUS, minecraft.options.getEffectiveRenderDistance());
      for (int x = playerChunkX - radius; x <= playerChunkX + radius; x++) {
         for (int z = playerChunkZ - radius; z <= playerChunkZ + radius; z++) {
            LevelChunk chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
            if (chunk != null) {
               SCAN_CHUNKS.add(chunk);
            }
         }
      }
      SCAN_CHUNKS.sort(Comparator.comparingLong(chunk -> chunk.getPos().distanceSquared(new ChunkPos(playerChunkX, playerChunkZ))));
      scannedLevel = level;
      chunkIndex = 0;
      blockIndex = 0;
      scanning = !SCAN_CHUNKS.isEmpty();
      status = scanning ? "Scanning " + SCAN_CHUNKS.size() + " loaded chunks..." : "No nearby loaded chunks to search.";
   }

   public static void tick(final Minecraft minecraft) {
      if (!scanning) {
         return;
      }
      if (minecraft.level == null || minecraft.player == null || minecraft.level != scannedLevel) {
         resetSearch("Block search stopped because the world changed.");
         return;
      }

      int playerChunkX = minecraft.player.chunkPosition().x();
      int playerChunkZ = minecraft.player.chunkPosition().z();
      if (Math.abs(playerChunkX - scanCenterX) > 2 || Math.abs(playerChunkZ - scanCenterZ) > 2) {
         MATCHES.clear();
         SCAN_CHUNKS.clear();
         prepareScan(minecraft);
         return;
      }

      int inspected = 0;
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
      while (scanning && inspected < SCAN_BUDGET_PER_FRAME) {
         if (chunkIndex >= SCAN_CHUNKS.size()) {
            scanning = false;
            status = "Found " + MATCHES.size() + " matches in loaded chunks.";
            break;
         }
         LevelChunk chunk = SCAN_CHUNKS.get(chunkIndex);
         int chunkBlockCount = scanHeight * 256;
         if (blockIndex >= chunkBlockCount) {
            chunkIndex++;
            blockIndex = 0;
            continue;
         }

         int localX = blockIndex & 15;
         int localZ = blockIndex >> 4 & 15;
         int y = minBuildHeight + (blockIndex >> 8);
         pos.set(chunk.getPos().getMinBlockX() + localX, y, chunk.getPos().getMinBlockZ() + localZ);
         if (SEARCH_BLOCKS.contains(chunk.getBlockState(pos).getBlock())) {
            MATCHES.add(pos.immutable());
            if (MATCHES.size() >= MAX_MATCHES) {
               scanning = false;
               status = "Showing the first " + MAX_MATCHES + " matches found in loaded chunks.";
            }
         }
         blockIndex++;
         inspected++;
      }
   }

   private static void resetSearch(final String message) {
      MATCHES.clear();
      SCAN_CHUNKS.clear();
      SEARCH_BLOCKS.clear();
      scannedLevel = null;
      scanning = false;
      status = message;
   }

   private static void clearSearch() {
      resetSearch("Block search cleared.");
      searchedText = "";
      Minecraft.getInstance().options.figClientBlockSearch().set("");
      Minecraft.getInstance().options.save();
   }

   public static List<BlockPos> matches() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null || minecraft.level != scannedLevel) {
         return List.of();
      }

      MATCHES.removeIf(pos -> {
         LevelChunk chunk = minecraft.level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);
         return chunk == null || !SEARCH_BLOCKS.contains(chunk.getBlockState(pos).getBlock());
      });
      return List.copyOf(MATCHES);
   }

   @Override
   public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      graphics.fill(0, 0, this.width, this.height, 0x26328CB8);
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      int centerX = this.width / 2;
      int centerY = this.height / 2;
      graphics.fill(centerX - 150, centerY - 88, centerX + 150, centerY - 86, 0xFF7CD8F3);
      graphics.centeredText(this.font, TITLE, centerX, centerY - 112, 0xFFFFFFFF);
      graphics.centeredText(this.font, Component.literal(status), centerX, centerY + 78, 0xFFB9D8E8);
      graphics.centeredText(this.font, Component.literal("Scans only loaded chunks within 4 chunks of you; up to 64 outlines."),
         centerX, centerY + 91, 0xFFB9D8E8);
      if (!searchedText.isEmpty()) {
         graphics.centeredText(this.font, Component.literal("Search: " + searchedText + (scanning ? " (scanning)" : "")),
            centerX, centerY + 104, 0xFF7CD8F3);
      }
   }

   @Override
   public void onClose() {
      this.minecraft.gui.setScreen(this.previousScreen);
   }
}
