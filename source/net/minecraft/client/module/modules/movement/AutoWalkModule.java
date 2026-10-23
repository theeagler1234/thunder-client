package net.minecraft.client.module.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.FloatValue;
import net.minecraft.world.entity.player.Player;

public class AutoWalkModule extends Module {
    private final FloatValue speed = registerValue(new FloatValue("Speed", "Walk speed multiplier", 1.0f, 0.1f, 5.0f, 0.1f));
    private final BooleanValue sprint = registerValue(new BooleanValue("Sprint", "Sprint while walking", false));
    private final BooleanValue jump = registerValue(new BooleanValue("Jump", "Auto jump", true));

    public AutoWalkModule() {
        super("AutoWalk", "Automatically walks forward", ModuleCategory.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || player.isDeadOrDying()) return;

        if (player.isMoving()) return; // Don't override manual movement

        double yaw = Math.toRadians(player.getYRot());
        double moveSpeed = 0.1 * speed.getValue();

        double motionX = -Math.sin(yaw) * moveSpeed;
        double motionZ = Math.cos(yaw) * moveSpeed;

        if (sprint.getValue() && player.isSprinting() == false) {
            player.setSprinting(true);
        }

        player.setDeltaMovement(motionX, player.getDeltaMovement().y, motionZ);

        if (jump.getValue() && player.onGround() && player.horizontalCollision) {
            player.jumpFromGround();
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player != null) {
            player.setSprinting(false);
        }
    }
}