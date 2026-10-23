package net.minecraft.client.module.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.EnumValue;
import net.minecraft.client.value.FloatValue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class FreecamModule extends Module {
    public enum Mode {
        Normal,
        Spectator,
        ThirdPerson
    }

    private final EnumValue<Mode> mode = registerValue(new EnumValue<>("Mode", "Freecam mode", Mode.Normal, List.of(Mode.values())));
    private final FloatValue speed = registerValue(new FloatValue("Speed", "Camera speed", 1.0f, 0.1f, 10.0f, 0.1f));
    private final FloatValue sprintMultiplier = registerValue(new FloatValue("SprintMultiplier", "Sprint speed multiplier", 2.0f, 1.0f, 5.0f, 0.1f));
    private final BooleanValue noClip = registerValue(new BooleanValue("NoClip", "No clip through blocks", true));
    private final BooleanValue sprint = registerValue(new BooleanValue("Sprint", "Allow sprint", true));
    private final BooleanValue thirdPerson = registerValue(new BooleanValue("ThirdPerson", "Use third person view", false));
    private final BooleanValue lockYaw = registerValue(new BooleanValue("LockYaw", "Lock camera yaw", false));

    private Vec3 cameraPos = Vec3.ZERO;
    private Vec3 cameraRot = Vec3.ZERO;
    private boolean wasThirdPerson = false;
    private boolean wasSprinting = false;

    public FreecamModule() {
        super("Freecam", "Detaches camera from player", ModuleCategory.RENDER);
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player != null) {
            cameraPos = new Vec3(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
            cameraRot = new Vec3(player.getXRot(), player.getYRot(), 0);
            wasThirdPerson = mc.options.getCameraType().isFirstPerson() == false;
            wasSprinting = player.isSprinting();

            if (thirdPerson.getValue()) {
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            } else {
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            }
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (wasThirdPerson) {
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        } else {
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
        }

        Player player = mc.player;
        if (player != null && wasSprinting) {
            player.setSprinting(true);
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        double moveSpeed = speed.getValue() * 0.1;
        if (sprint.getValue() && mc.options.keySprint.isDown()) {
            moveSpeed *= sprintMultiplier.getValue();
        }

        double forward = 0, right = 0, up = 0;

        if (mc.options.keyUp.isDown()) forward = 1;
        if (mc.options.keyDown.isDown()) forward = -1;
        if (mc.options.keyRight.isDown()) right = 1;
        if (mc.options.keyLeft.isDown()) right = -1;
        if (mc.options.keyJump.isDown()) up = 1;
        if (mc.options.keyShift.isDown()) up = -1;

        double yaw = Math.toRadians((float) cameraRot.y);
        double pitch = Math.toRadians((float) cameraRot.x);

        double motionX = forward * -Math.sin(yaw) * Math.cos(pitch) + right * Math.cos(yaw);
        double motionY = forward * -Math.sin(pitch) + up;
        double motionZ = forward * Math.cos(yaw) * Math.cos(pitch) + right * Math.sin(yaw);

        Vec3 newPos = cameraPos.add(motionX * moveSpeed, motionY * moveSpeed, motionZ * moveSpeed);

        if (noClip.getValue() || mode.getValue() == Mode.Spectator) {
            cameraPos = newPos;
        } else {
            // Check for collisions (simplified)
            cameraPos = newPos;
        }

        if (mc.gameRenderer != null && mc.gameRenderer.mainCamera() != null) {
            mc.gameRenderer.mainCamera().setPosition(cameraPos);
            if (!lockYaw.getValue()) {
                mc.gameRenderer.mainCamera().setRotation((float) cameraRot.x, (float) cameraRot.y);
            }
        }
    }

    @Override
    public void onKeyPress(int key) {
        Minecraft mc = Minecraft.getInstance();
        // Handle mouse look for camera rotation
    }
}