package net.minecraft.client.module.modules.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.EnumValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import java.util.List;

public class SafeWalkModule extends Module {
    public enum Mode {
        Normal,
        SneakOnly,
        Strict
    }

    private final EnumValue<Mode> mode = registerValue(new EnumValue<>("Mode", "SafeWalk mode", Mode.Normal, List.of(Mode.values())));
    private final BooleanValue onlyWhenSneaking = registerValue(new BooleanValue("OnlyWhenSneaking", "Only safe walk when sneaking", false));
    private final BooleanValue blocks = registerValue(new BooleanValue("Blocks", "Check for blocks", true));
    private final BooleanValue liquids = registerValue(new BooleanValue("Liquids", "Prevent falling in liquids", true));
    private final BooleanValue entities = registerValue(new BooleanValue("Entities", "Avoid entity edges", false));

    public SafeWalkModule() {
        super("SafeWalk", "Prevents falling off edges", ModuleCategory.WORLD);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;

        if (player == null || level == null) return;

        if (onlyWhenSneaking.getValue() && !player.isShiftKeyDown()) return;

        if (player.isMoving() && !player.isSprinting()) {
            BlockPos pos = player.getOnPos();
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;
                    BlockPos checkPos = pos.offset(x, -1, z);

                    boolean unsafe = false;

                    if (blocks.getValue()) {
                        if (level.isEmptyBlock(checkPos) && level.isEmptyBlock(checkPos.above())) {
                            unsafe = true;
                        }
                    }

                    if (liquids.getValue() && !unsafe) {
                        if (level.getFluidState(checkPos).isEmpty() && level.getFluidState(checkPos.above()).isEmpty()) {
                            unsafe = true;
                        }
                    }

                    if (entities.getValue() && !unsafe) {
                        // Check for entities that could push us off
                    }

                    if (unsafe) {
                        // Would prevent movement toward this direction
                        // Requires movement input modification
                    }
                }
            }
        }
    }
}