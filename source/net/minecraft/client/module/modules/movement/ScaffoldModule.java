package net.minecraft.client.module.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.EnumValue;
import net.minecraft.client.value.FloatValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ScaffoldModule extends Module {
    public enum Mode {
        Normal,
        Swing,
        Tower,
        Down
    }

    private final EnumValue<Mode> mode = registerValue(new EnumValue<>("Mode", "Scaffold mode", Mode.Normal, List.of(Mode.values())));
    private final FloatValue expand = registerValue(new FloatValue("Expand", "Block expand length", 5.0f, 1.0f, 10.0f, 0.5f));
    private final BooleanValue rotate = registerValue(new BooleanValue("Rotate", "Rotate to place", true));
    private final BooleanValue sprint = registerValue(new BooleanValue("Sprint", "Sprint while scaffolding", false));
    private final BooleanValue safeWalk = registerValue(new BooleanValue("SafeWalk", "Don't fall off edges", true));

    public ScaffoldModule() {
        super("Scaffold", "Automatically places blocks under you", ModuleCategory.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;

        if (player == null || level == null) return;
        if (player.isSpectator() || player.isCreative()) return;

        BlockPos playerPos = player.blockPosition();
        List<BlockPos> positionsToPlace = getScaffoldPositions(player, level, playerPos);

        if (!positionsToPlace.isEmpty() && hasBlocksInInventory(player)) {
            for (BlockPos pos : positionsToPlace) {
                if (mc.gameMode != null) {
                    Direction face = getPlaceFace(player, pos);
                    mc.gameMode.placeBlock(player.getUsedItemHand(), pos, face);
                    break;
                }
            }
        }
    }

    private List<BlockPos> getScaffoldPositions(Player player, Level level, BlockPos playerPos) {
        List<BlockPos> positions = new ArrayList<>();

        switch (mode.getValue()) {
            case Normal -> {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && z == 0) continue;
                        BlockPos pos = playerPos.offset(x, -1, z);
                        if (canPlace(level, pos)) positions.add(pos);
                    }
                }
            }
            case Swing -> {
                BlockPos below = playerPos.below();
                if (canPlace(level, below)) positions.add(below);
            }
            case Tower -> {
                if (player.isMoving() && canPlace(level, playerPos.below())) {
                    positions.add(playerPos.below());
                }
            }
            case Down -> {
                if (Minecraft.getInstance().options.keyShift.isDown() && canPlace(level, playerPos.below())) {
                    positions.add(playerPos.below());
                }
            }
        }

        // Expand
        for (int i = 1; i < expand.getValue(); i++) {
            List<BlockPos> expanded = new ArrayList<>();
            for (BlockPos pos : positions) {
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockPos newPos = pos.relative(dir);
                    if (canPlace(level, newPos)) expanded.add(newPos);
                }
            }
            positions.addAll(expanded);
        }

        // Sort by distance
        positions.sort(Comparator.comparingDouble(pos -> pos.distSqr(playerPos)));

        return positions;
    }

    private boolean canPlace(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.canBeReplaced()) return false;
        if (state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.LAVA) return false;

        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (belowState.canBeReplaced()) return false;

        return true;
    }

    private boolean hasBlocksInInventory(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.BlockItem) {
                return true;
            }
        }
        return false;
    }

    private Direction getPlaceFace(Player player, BlockPos pos) {
        Vec3 playerPos = new Vec3(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
        Vec3 blockPos = new Vec3(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        Vec3 diff = blockPos.subtract(playerPos).normalize();

        double max = Math.max(Math.abs(diff.x), Math.max(Math.abs(diff.y), Math.abs(diff.z)));
        if (max == Math.abs(diff.x)) return diff.x > 0 ? Direction.EAST : Direction.WEST;
        if (max == Math.abs(diff.y)) return diff.y > 0 ? Direction.UP : Direction.DOWN;
        return diff.z > 0 ? Direction.SOUTH : Direction.NORTH;
    }
}