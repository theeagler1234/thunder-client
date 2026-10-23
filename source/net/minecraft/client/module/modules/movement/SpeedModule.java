package net.minecraft.client.module.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.EnumValue;
import net.minecraft.client.value.FloatValue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import java.util.List;

public class SpeedModule extends Module {
    public enum Mode {
        Vanilla,
        Bounce,
        Timer,
        Strafe
    }

    private final EnumValue<Mode> mode = registerValue(new EnumValue<>("Mode", "Speed mode", Mode.Vanilla, List.of(Mode.values())));
    private final FloatValue speed = registerValue(new FloatValue("Speed", "Speed multiplier", 2.5f, 0.5f, 10.0f, 0.1f));
    private final BooleanValue strafe = registerValue(new BooleanValue("Strafe", "Enable strafing", true));
    private final BooleanValue autoJump = registerValue(new BooleanValue("AutoJump", "Auto jump on blocks", true));

    private double lastDist = 0;

    public SpeedModule() {
        super("Speed", "Increases movement speed", ModuleCategory.MOVEMENT);
    }

    @Override
    public void onEnable() {
        lastDist = 0;
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player != null) {
            player.getAbilities().setWalkingSpeed(0.1f);
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || player.isDeadOrDying()) return;

        if (player.isSpectator() || player.isCreative()) return;

        switch (mode.getValue()) {
            case Vanilla -> vanillaSpeed(player);
            case Bounce -> bounceSpeed(player);
            case Timer -> timerSpeed(player);
            case Strafe -> strafeSpeed(player);
        }
    }

    private void vanillaSpeed(Player player) {
        if (player.isMoving() && player.onGround()) {
            double moveSpeed = 0.2873 * speed.getValue();
            double yaw = Math.toRadians(player.getYRot());
            double strafeYaw = yaw + Math.PI / 2;

            double forward = player.zza;
            double strafeVal = player.xxa;

            if (strafe.getValue() && strafeVal != 0) {
                if (forward > 0) {
                    yaw += strafeVal > 0 ? -Math.PI / 4 : Math.PI / 4;
                } else if (forward < 0) {
                    yaw += strafeVal > 0 ? Math.PI / 4 : -Math.PI / 4;
                }
                forward = 0;
            }

            double motionX = forward * moveSpeed * -Math.sin(yaw) + strafeVal * moveSpeed * Math.cos(yaw);
            double motionZ = forward * moveSpeed * Math.cos(yaw) + strafeVal * moveSpeed * Math.sin(yaw);

            player.setDeltaMovement(motionX, player.getDeltaMovement().y, motionZ);
        }
    }

    private void bounceSpeed(Player player) {
        if (player.isMoving()) {
            double moveSpeed = 0.2873 * speed.getValue();
            if (player.onGround()) {
                player.setDeltaMovement(player.getDeltaMovement().x * 1.1, 0.42f, player.getDeltaMovement().z * 1.1);
            }
            double yaw = Math.toRadians(player.getYRot());
            player.setDeltaMovement(
                -Math.sin(yaw) * moveSpeed,
                player.getDeltaMovement().y,
                Math.cos(yaw) * moveSpeed
            );
        }
    }

    private void timerSpeed(Player player) {
        Minecraft mc = Minecraft.getInstance();
        mc.timer = speed.getValue();
    }

    private void strafeSpeed(Player player) {
        if (player.isMoving()) {
            double moveSpeed = 0.2873 * speed.getValue();
            double yaw = Math.toRadians(player.getYRot());

            double forward = player.zza;
            double strafeVal = player.xxa;

            double motionX = forward * moveSpeed * -Math.sin(yaw) + strafeVal * moveSpeed * Math.cos(yaw);
            double motionZ = forward * moveSpeed * Math.cos(yaw) + strafeVal * moveSpeed * Math.sin(yaw);

            if (player.onGround() && autoJump.getValue() && player.horizontalCollision) {
                player.jumpFromGround();
            }

            player.setDeltaMovement(motionX, player.getDeltaMovement().y, motionZ);
        }
    }
}