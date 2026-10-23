package net.minecraft.client.module.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.EnumValue;
import net.minecraft.client.value.FloatValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.Comparator;

public class KillAuraModule extends Module {
    public enum Priority {
        Closest,
        LowestHealth,
        HighestDamage,
        LookingAt
    }

    public enum RotateMode {
        Off,
        Silent,
        Normal
    }

    private final FloatValue range = registerValue(new FloatValue("Range", "Attack range in blocks", 4.5f, 1.0f, 6.0f, 0.1f));
    private final FloatValue aps = registerValue(new FloatValue("APS", "Attacks per second", 12.0f, 1.0f, 20.0f, 0.5f));
    private final BooleanValue throughWalls = registerValue(new BooleanValue("ThroughWalls", "Attack through walls", false));
    private final BooleanValue players = registerValue(new BooleanValue("Players", "Target players", true));
    private final BooleanValue animals = registerValue(new BooleanValue("Animals", "Target animals", false));
    private final BooleanValue mobs = registerValue(new BooleanValue("Mobs", "Target hostile mobs", true));
    private final BooleanValue invisibles = registerValue(new BooleanValue("Invisibles", "Target invisible entities", false));
    private final EnumValue<Priority> priority = registerValue(new EnumValue<>("Priority", "Target selection priority", Priority.Closest, List.of(Priority.values())));
    private final EnumValue<RotateMode> rotateMode = registerValue(new EnumValue<>("RotateMode", "Rotation mode", RotateMode.Silent, List.of(RotateMode.values())));
    private final BooleanValue swingHand = registerValue(new BooleanValue("SwingHand", "Swing hand client-side", true));
    private final BooleanValue autoBlock = registerValue(new BooleanValue("AutoBlock", "Auto block with sword", true));
    private final FloatValue minDamage = registerValue(new FloatValue("MinDamage", "Minimum damage to deal", 0.5f, 0.0f, 10.0f, 0.5f));

    private int attackCooldown = 0;
    private LivingEntity lastTarget = null;

    public KillAuraModule() {
        super("KillAura", "Automatically attacks nearby entities", ModuleCategory.COMBAT);
    }

    @Override
    public void onEnable() {
        attackCooldown = 0;
        lastTarget = null;
    }

    @Override
    public void onDisable() {
        lastTarget = null;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || player.isDeadOrDying()) return;

        if (attackCooldown > 0) {
            attackCooldown--;
            return;
        }

        LivingEntity target = findTarget(player);
        if (target != null) {
            attackTarget(player, target);
            attackCooldown = Math.max(1, (int) (20.0f / aps.getValue()));
        }
    }

    private LivingEntity findTarget(Player player) {
        double rangeSq = range.getValue() * range.getValue();
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class,
            new AABB(player.getX() - range.getValue(), player.getY() - range.getValue(), player.getZ() - range.getValue(),
                     player.getX() + range.getValue(), player.getY() + range.getValue(), player.getZ() + range.getValue()),
            e -> isValidTarget(player, e));

        if (entities.isEmpty()) return null;

        return switch (priority.getValue()) {
            case Closest -> entities.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);
            case LowestHealth -> entities.stream()
                .min(Comparator.comparingDouble(LivingEntity::getHealth))
                .orElse(null);
            case HighestDamage -> entities.stream()
                .max(Comparator.comparingDouble(this::calculateDamage))
                .orElse(null);
            case LookingAt -> entities.stream()
                .min(Comparator.comparingDouble(e -> getRotationDifference(player, e)))
                .orElse(null);
        };
    }

    private boolean isValidTarget(Player player, LivingEntity entity) {
        if (entity == player) return false;
        if (entity.isDeadOrDying()) return false;
        if (!entity.isAlive()) return false;
        if (entity.getHealth() <= 0) return false;
        if (entity.isInvulnerable()) return false;
        if (entity.distanceToSqr(player) > range.getValue() * range.getValue()) return false;

        if (entity.isInvisible() && !invisibles.getValue()) return false;

        boolean isPlayer = entity instanceof Player;
        boolean isAnimal = !isPlayer && isPassive(entity);
        boolean isMob = !isPlayer && !isAnimal;

        if (isPlayer && !players.getValue()) return false;
        if (isAnimal && !animals.getValue()) return false;
        if (isMob && !mobs.getValue()) return false;

        if (!throughWalls.getValue() && !player.hasLineOfSight(entity)) return false;

        return true;
    }

    private boolean isPassive(LivingEntity entity) {
        return entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.CREATURE ||
               entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.AMBIENT ||
               entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.WATER_CREATURE;
    }

    private double calculateDamage(LivingEntity target) {
        return 1.0;
    }

    private double getRotationDifference(Player player, LivingEntity target) {
        double diffX = target.getX() - player.getX();
        double diffZ = target.getZ() - player.getZ();
        double yawToEntity = Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0;
        double pitchToEntity = Math.toDegrees(-Math.atan2(target.getY() + target.getEyeHeight() - (player.getY() + player.getEyeHeight()),
            Math.sqrt(diffX * diffX + diffZ * diffZ)));

        double yawDiff = Math.abs(player.getYRot() - yawToEntity) % 360;
        double pitchDiff = Math.abs(player.getXRot() - pitchToEntity);

        return Math.min(yawDiff, 360 - yawDiff) + pitchDiff;
    }

    private void attackTarget(Player player, LivingEntity target) {
        if (rotateMode.getValue() != RotateMode.Off) {
            rotateToTarget(player, target);
        }

        player.swing(player.getUsedItemHand());

        if (mc.gameMode != null) {
            mc.gameMode.attack(player, target);
        }

        lastTarget = target;
    }

    private void rotateToTarget(Player player, LivingEntity target) {
        double diffX = target.getX() - player.getX();
        double diffY = target.getY() + target.getEyeHeight() - (player.getY() + player.getEyeHeight());
        double diffZ = target.getZ() - player.getZ();

        double distance = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, distance));

        if (rotateMode.getValue() == RotateMode.Silent) {
            // Silent rotation - only send rotation packets, don't change client view
            // This would require packet manipulation
        } else {
            player.setYRot(yaw);
            player.setXRot(pitch);
        }
    }
}