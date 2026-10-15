package net.minecraft.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

public final class FigBlueprintManager {
   private static final int MAX_BLOCKS = 8192;
   private static final int MAX_FILE_BYTES = 16 * 1024 * 1024;
   private static final double MAX_RENDER_DISTANCE_SQUARED = 2304.0;
   private static List<StructureTemplate.StructureBlockInfo> blocks = List.of();
   private static BlockPos origin;
   private static net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;

   private FigBlueprintManager() {
   }

   public static void load(final byte[] data, final ClientLevel level, final BlockPos origin) throws IOException {
      if (data.length > MAX_FILE_BYTES) {
         throw new IOException("Blueprint files must be smaller than 16 MiB");
      }

      StructureTemplate template = new StructureTemplate();
      template.load(
         level.registryAccess().lookupOrThrow(Registries.BLOCK),
         NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.create(64L * 1024L * 1024L))
      );
      List<StructureTemplate.StructureBlockInfo> blockInfo = template.getBlockInfo();
      if (blockInfo.isEmpty() || blockInfo.size() > MAX_BLOCKS) {
         throw new IOException("Blueprint must contain between 1 and " + MAX_BLOCKS + " blocks");
      }

      FigBlueprintManager.blocks = blockInfo;
      FigBlueprintManager.origin = origin;
      FigBlueprintManager.dimension = level.dimension();
   }

   public static void clear() {
      blocks = List.of();
      origin = null;
      dimension = null;
   }

   public static boolean hasBlueprint() {
      return !blocks.isEmpty();
   }

   public static void extract(final ClientLevel level, final Player player) {
      if (blocks.isEmpty() || origin == null || !level.dimension().equals(dimension)) {
         return;
      }

      double maxDistance = MAX_RENDER_DISTANCE_SQUARED;
      for (StructureTemplate.StructureBlockInfo block : blocks) {
         BlockPos target = origin.offset(block.pos());
         if (target.distToCenterSqr(player.getX(), player.getY(), player.getZ()) <= maxDistance
            && !level.getBlockState(target).equals(block.state())) {
            boolean replaceExisting = !level.getBlockState(target).isAir();
            int color = replaceExisting ? 0xFFFFA040 : 0xFF45E0D0;
            Gizmos.cuboid(
               new AABB(target).deflate(0.025),
               GizmoStyle.strokeAndFill(color, 1.5F, (replaceExisting ? 0x38FF9A38 : 0x3845E0D0))
            ).setAlwaysOnTop();
         }
      }
   }
}