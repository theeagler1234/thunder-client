package net.minecraft.client.module.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.FloatValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.EndCrystalEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Comparator;

public class AutoCrystalModule extends Module {
    private final FloatValue placeRange = registerValue(new FloatValue("PlaceRange", "Crystal place range", 5.0f, 1.0f, 6.0f, 0.1f));
    private final FloatValue breakRange = registerValue(new FloatValue("BreakRange", "Crystal break range", 5.0f, 1.0f, 6.0f, 0.1f));
    private final BooleanValue rotate = registerValue(new BooleanValue("Rotate", "Rotate to target", true));
    private final FloatValue minDamage = registerValue(new FloatValue("MinDamage", "Minimum damage to target", 4.0f, 0.5f, 20.0f, 0.5f));
    private final FloatValue maxSelfDamage = registerValue(new FloatValue("MaxSelfDamage", "Maximum self damage", 4.0f, 0.5f, 20.0f, 0.5f));
    private final BooleanValue facePlace = registerValue(new BooleanValue("FacePlace", "Place on face", true));
    private final BooleanValue antiWeakness = registerValue(new BooleanValue("AntiWeakness", "Don't break with weakness", true));
    private final BooleanValue packetBreak = registerValue(new BooleanValue("PacketBreak", "Use packet breaking", true));
    private final IntegerValue placeDelay = registerValue(new IntegerValue("PlaceDelay", "Delay between places (ticks)", 0, 0, 10));
    private final IntegerValue breakDelay = registerValue(new IntegerValue("BreakDelay", "Delay between breaks (ticks)", 0, 0, 10));

    private int placeCooldown = 0;
    private int breakCooldown = 0;
    private BlockPos lastPlacePos = null;

    public AutoCrystalModule() {
        super("AutoCrystal", "Automatically places and breaks end crystals", ModuleCategory.COMBAT);
    }

    @Override
    public void onEnable() {
        placeCooldown = 0;
        breakCooldown = 0;
        lastPlacePos = null;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;

        if (player == null || level == null) return;

        if (placeCooldown > 0) placeCooldown--;
        if (breakCooldown > 0) breakCooldown--;

        if (placeCooldown <= 0) {
            placeCrystal(player, level);
        }

        if (breakCooldown <= 0) {
            breakCrystal(player, level);
        }
    }

    private void placeCrystal(Player player, Level level) {
        if (!hasCrystalInInventory(player)) return;

        BlockPos bestPos = findBestPlacePosition(player, level);
        if (bestPos == null) return;

        if (mc.gameMode != null) {
            mc.gameMode.placeBlock(player.getUsedItemHand(), bestPos);
            placeCooldown = placeDelay.getValue();
            lastPlacePos = bestPos;
        }
    }

    private void breakCrystal(Player player, Level level) {
        List<EndCrystalEntity> crystals = level.getEntitiesOfClass(EndCrystalEntity.class,
            new AABB(player.getX() - breakRange.getValue(), player.getY() - breakRange.getValue(), player.getZ() - breakRange.getValue(),
                     player.getX() + breakRange.getValue(), player.getY() + breakRange.getValue(), player.getZ() + breakRange.getValue()),
            e -> e.isAlive() && e.distanceToSqr(player) <= breakRange.getValue() * breakRange.getValue());

        if (crystals.isEmpty()) return;

        EndCrystalEntity bestCrystal = crystals.stream()
            .max(Comparator.comparingDouble(c -> calculateCrystalDamage(c, player)))
            .orElse(null);

        if (bestCrystal == null) return;

        double damage = calculateCrystalDamage(bestCrystal, player);
        if (damage < minDamage.getValue()) return;

        if (antiWeakness.getValue() && player.hasEffect(MobEffects.WEAKNESS)) return;

        if (mc.gameMode != null) {
            mc.gameMode.attack(player, bestCrystal);
            breakCooldown = breakDelay.getValue();
        }
    }

    private boolean hasCrystalInInventory(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == Items.END_CRYSTAL) return true;
        }
        return false;
    }

    private BlockPos findBestPlacePosition(Player player, Level level) {
        List<Player> targets = level.getEntitiesOfClass(Player.class,
            new AABB(player.getX() - placeRange.getValue(), player.getY() - placeRange.getValue(), player.getZ() - placeRange.getValue(),
                     player.getX() + placeRange.getValue(), player.getY() + placeRange.getValue(), player.getZ() + placeRange.getValue()),
            p -> p != player && p.isAlive() && p.distanceToSqr(player) <= placeRange.getValue() * placeRange.getValue());

        if (targets.isEmpty()) return null;

        BlockPos bestPos = null;
        double bestDamage = 0;

        for (Player target : targets) {
            for (BlockPos pos : getPlaceablePositions(target.blockPosition())) {
                if (!canPlaceCrystal(level, pos)) continue;
                if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5) > placeRange.getValue() * placeRange.getValue()) continue;

                double damage = calculateExplosionDamage(pos, target);
                double selfDamage = calculateExplosionDamage(pos, player);

                if (damage >= minDamage.getValue() && selfDamage <= maxSelfDamage.getValue()) {
                    if (damage > bestDamage) {
                        bestDamage = damage;
                        bestPos = pos;
                    }
                }
            }
        }

        return bestPos;
    }

    private List<BlockPos> getPlaceablePositions(BlockPos center) {
        List<BlockPos> positions = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN) continue;
            if (!facePlace.getValue() && dir == Direction.UP) continue;
            positions.add(center.relative(dir));
        }
        positions.add(center.above());
        return positions;
    }

    private boolean canPlaceCrystal(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState below = level.getBlockState(pos.below());

        if (!state.canBeReplaced()) return false;
        if (!(below.getBlock() == Blocks.BEDROCK || below.getBlock() == Blocks.OBSIDIAN)) return false;

        for (Entity entity : level.getEntities(null, new AABB(pos))) {
            if (entity instanceof EndCrystalEntity) return false;
        }

        return true;
    }

    private double calculateCrystalDamage(EndCrystalEntity crystal, LivingEntity target) {
        return calculateExplosionDamage(crystal.blockPosition(), target);
    }

    private double calculateExplosionDamage(BlockPos pos, LivingEntity entity) {
        double distance = entity.distanceToSqr(new Vec3(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5));
        if (distance > 12 * 12) return 0;

        double exposure = getExposure(pos, entity);
        double damage = (1 - Math.sqrt(distance) / 12) * exposure * 6;
        return damage;
    }

    private double getExposure(BlockPos pos, LivingEntity entity) {
        Vec3 crystalPos = new Vec3(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        Vec3 entityPos = entity.getEyePosition();

        return entity.level().clip(new ClipContext(crystalPos, entityPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getBlockPos().equals(pos) ? 1.0 : 0.0;
    }
}