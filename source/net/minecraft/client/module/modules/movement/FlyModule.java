package net.minecraft.client.module.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.EnumValue;
import net.minecraft.client.value.FloatValue;
import net.minecraft.world.entity.player.Player;
import java.util.List;

public class FlyModule extends Module {
    public enum Mode {
        Vanilla,
        Creative,
        Damage,
        Glide
    }

    private final EnumValue<Mode> mode = registerValue(new EnumValue<>("Mode", "Fly mode", Mode.Vanilla, List.of(Mode.values())));
    private final FloatValue speed = registerValue(new FloatValue("Speed", "Fly speed", 3.0f, 0.5f, 10.0f, 0.1f));
    private final BooleanValue creative = registerValue(new BooleanValue("Creative", "Use creative flight", false));
    private final BooleanValue noKick = registerValue(new BooleanValue("NoKick", "Anti-kick", true));

    private double lastY = 0;

    public FlyModule() {
        super("Fly", "Allows flying in survival", ModuleCategory.MOVEMENT);
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player != null) {
            lastY = player.getY();
            if (creative.getValue() || mode.getValue() == Mode.Creative) {
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            }
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player != null && !player.isCreative()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || player.isDeadOrDying()) return;

        if (creative.getValue() || mode.getValue() == Mode.Creative) {
            if (!player.getAbilities().flying) {
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            }
            return;
        }

        switch (mode.getValue()) {
            case Vanilla -> vanillaFly(player);
            case Damage -> damageFly(player);
            case Glide -> glideFly(player);
        }
    }

    private void vanillaFly(Player player) {
        double moveSpeed = speed.getValue() * 0.1;
        double yaw = Math.toRadians(player.getYRot());

        double motionX = 0, motionY = 0, motionZ = 0;

        if (player.zza > 0) {
            motionX -= Math.sin(yaw) * moveSpeed;
            motionZ += Math.cos(yaw) * moveSpeed;
        }
        if (player.zza < 0) {
            motionX += Math.sin(yaw) * moveSpeed;
            motionZ -= Math.cos(yaw) * moveSpeed;
        }
        if (player.xxa > 0) {
            motionX += Math.cos(yaw) * moveSpeed;
            motionZ += Math.sin(yaw) * moveSpeed;
        }
        if (player.xxa < 0) {
            motionX -= Math.cos(yaw) * moveSpeed;
            motionZ -= Math.sin(yaw) * moveSpeed;
        }

        if (Minecraft.getInstance().options.keyJump.isDown()) {
            motionY = moveSpeed;
        }
        if (Minecraft.getInstance().options.keyShift.isDown()) {
            motionY = -moveSpeed;
        }

        if (motionX == 0 && motionY == 0 && motionZ == 0) {
            player.setDeltaMovement(0, 0, 0);
        } else {
            player.setDeltaMovement(motionX, motionY, motionZ);
        }

        player.fallDistance = 0;
    }

    private void damageFly(Player player) {
        if (player.tickCount % 3 == 0) {
            player.hurt(player.damageSources().generic(), 0);
            player.setDeltaMovement(player.getDeltaMovement().x, 0.42, player.getDeltaMovement().z);
        }
        vanillaFly(player);
    }

    private void glideFly(Player player) {
        if (!player.onGround()) {
            double motionY = player.getDeltaMovement().y * 0.98;
            if (Minecraft.getInstance().options.keyJump.isDown()) {
                motionY = 0.3;
            }
            player.setDeltaMovement(player.getDeltaMovement().x, motionY, player.getDeltaMovement().z);
        }
        vanillaFly(player);
    }
}