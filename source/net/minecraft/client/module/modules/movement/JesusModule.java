package net.minecraft.client.module.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public class JesusModule extends Module {
    private final BooleanValue lava = registerValue(new BooleanValue("Lava", "Walk on lava", true));
    private final BooleanValue water = registerValue(new BooleanValue("Water", "Walk on water", true));
    private final BooleanValue sprint = registerValue(new BooleanValue("Sprint", "Allow sprinting", true));

    public JesusModule() {
        super("Jesus", "Walk on water/lava", ModuleCategory.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;

        if (player == null || level == null) return;

        BlockPos belowPos = player.getOnPos().below();
        BlockState belowState = level.getBlockState(belowPos);
        Fluid belowFluid = belowState.getFluidState().getType();

        boolean onWater = belowFluid.isSame(net.minecraft.world.level.material.Fluids.WATER);
        boolean onLava = belowFluid.isSame(net.minecraft.world.level.material.Fluids.LAVA);

        if ((onWater && water.getValue()) || (onLava && lava.getValue())) {
            if (!player.isSpectator() && !player.isCreative()) {
                player.setDeltaMovement(player.getDeltaMovement().x, 0, player.getDeltaMovement().z);
                if (player.isSprinting() && !sprint.getValue()) {
                    player.setSprinting(false);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        // Nothing needed
    }
}