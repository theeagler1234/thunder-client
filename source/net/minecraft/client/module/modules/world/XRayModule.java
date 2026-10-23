package net.minecraft.client.module.modules.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.StringValue;
import net.minecraft.client.renderer.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import java.util.Random;

public class XRayModule extends Module {
    private final BooleanValue ores = registerValue(new BooleanValue("Ores", "Show ores", true));
    private final BooleanValue chests = registerValue(new BooleanValue("Chests", "Show chests", true));
    private final BooleanValue spawners = registerValue(new BooleanValue("Spawners", "Show spawners", true));
    private final BooleanValue nether = registerValue(new BooleanValue("Nether", "Nether ores", true));
    private final BooleanValue antiXrayBypass = registerValue(new BooleanValue("AntiXrayBypass", "Bypass anti-xray with seed", false));
    private final StringValue seedInput = registerValue(new StringValue("Seed", "World seed for anti-xray bypass", "", 20));
    private final BooleanValue useCustomSeed = registerValue(new BooleanValue("UseCustomSeed", "Use custom seed instead of auto", false));

    private long cachedSeed = 0;
    private boolean seedCached = false;

    private static final net.minecraft.core.HolderSet<net.minecraft.world.level.block.Block> ORE_BLOCKS = net.minecraft.core.HolderSet.direct(
        net.minecraft.world.level.block.Blocks.COAL_ORE.builtInRegistryHolder(),
        net.minecraft.world.level.block.Blocks.IRON_ORE.builtInRegistryHolder(),
        net.minecraft.world.level.block.Blocks.GOLD_ORE.builtInRegistryHolder(),
        net.minecraft.world.level.block.Blocks.DIAMOND_ORE.builtInRegistryHolder(),
        net.minecraft.world.level.block.Blocks.EMERALD_ORE.builtInRegistryHolder(),
        net.minecraft.world.level.block.Blocks.REDSTONE_ORE.builtInRegistryHolder(),
        net.minecraft.world.level.block.Blocks.LAPIS_ORE.builtInRegistryHolder(),
        net.minecraft.world.level.block.Blocks.COPPER_ORE.builtInRegistryHolder(),
        net.minecraft.world.level.block.Blocks.DEEPSLATE_COAL_ORE.builtInRegistryHolder(),
        net.minecraft.world.level.block.Blocks.DEEPSLATE_IRON_ORE.builtInRegistryHolder(),
        net.minecraft.world.level.block.Blocks.DEEPSLATE_GOLD_ORE.builtInRegistryHolder(),
        net.minecraft.world.level.block.Blocks.DEEPSLATE_DIAMOND_ORE.builtInRegistryHolder(),
        net.minecraft.world.level.block.Blocks.DEEPSLATE_EMERALD_ORE.builtInRegistryHolder(),
        net.minecraft.world.level.block.Blocks.DEEPSLATE_REDSTONE_ORE.builtInRegistryHolder(),
        net.minecraft.world.level.block.Blocks.DEEPSLATE_LAPIS_ORE.builtInRegistryHolder(),
        net.minecraft.world.level.block.Blocks.DEEPSLATE_COPPER_ORE.builtInRegistryHolder(),
        net.minecraft.world.level.block.Blocks.NETHER_GOLD_ORE.builtInRegistryHolder(),
        net.minecraft.world.level.block.Blocks.NETHER_QUARTZ_ORE.builtInRegistryHolder(),
        net.minecraft.world.level.block.Blocks.ANCIENT_DEBRIS.builtInRegistryHolder()
    );

    public XRayModule() {
        super("XRay", "See through blocks", ModuleCategory.WORLD);
    }

    @Override
    public void onEnable() {
        seedCached = false;
        cachedSeed = 0;
    }

    @Override
    public void onDisable() {
        // Restore normal rendering
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        if (antiXrayBypass.getValue() && !seedCached) {
            long seed = getWorldSeed(mc);
            if (seed != 0) {
                cachedSeed = seed;
                seedCached = true;
            }
        }
    }

    private long getWorldSeed(Minecraft mc) {
        if (useCustomSeed.getValue() && !seedInput.getValue().isEmpty()) {
            try {
                return Long.parseLong(seedInput.getValue());
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        if (mc.level != null) {
            return mc.level.getSeed();
        }
        return 0;
    }

    public boolean isOreAtPosition(BlockPos pos) {
        if (!antiXrayBypass.getValue() || cachedSeed == 0) return true;

        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        Random rand = new Random(cachedSeed + chunkX * 10387313 ^ chunkZ * 38472913);

        int localX = pos.getX() & 15;
        int localZ = pos.getZ() & 15;

        for (var holder : ORE_BLOCKS) {
            if (holder.is(Blocks.DIAMOND_ORE) || holder.is(Blocks.DEEPSLATE_DIAMOND_ORE) ||
                holder.is(Blocks.EMERALD_ORE) || holder.is(Blocks.ANCIENT_DEBRIS)) {
                if (rand.nextInt(100) < 2) return true;
            }
        }
        return false;
    }

    public boolean isXRayBlock(net.minecraft.world.level.block.state.BlockState state) {
        if (ores.getValue() && ORE_BLOCKS.contains(state.getBlock())) return true;
        if (chests.getValue() && state.getBlock() == Blocks.CHEST) return true;
        if (chests.getValue() && state.getBlock() == Blocks.ENDER_CHEST) return true;
        if (chests.getValue() && state.getBlock() == Blocks.TRAPPED_CHEST) return true;
        if (chests.getValue() && state.getBlock() == Blocks.BARREL) return true;
        if (spawners.getValue() && state.getBlock() == Blocks.SPAWNER) return true;
        return false;
    }

    public boolean isXRayBlockEntity(BlockEntity entity) {
        if (chests.getValue() && entity instanceof ChestBlockEntity) return true;
        if (spawners.getValue() && entity instanceof SpawnerBlockEntity) return true;
        return false;
    }

    public long getCachedSeed() {
        return cachedSeed;
    }

    public boolean hasSeed() {
        return seedCached && cachedSeed != 0;
    }
}