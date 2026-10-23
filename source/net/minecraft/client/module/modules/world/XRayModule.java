package net.minecraft.client.module.modules.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.renderer.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

public class XRayModule extends Module {
    private final BooleanValue ores = registerValue(new BooleanValue("Ores", "Show ores", true));
    private final BooleanValue chests = registerValue(new BooleanValue("Chests", "Show chests", true));
    private final BooleanValue spawners = registerValue(new BooleanValue("Spawners", "Show spawners", true));
    private final BooleanValue nether = registerValue(new BooleanValue("Nether", "Nether ores", true));

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
        // X-Ray is typically implemented by modifying the block render layer
        // or by using a custom shader
    }

    @Override
    public void onDisable() {
        // Restore normal rendering
    }

    @Override
    public void onTick() {
        // X-Ray is mostly a render-time effect
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
}