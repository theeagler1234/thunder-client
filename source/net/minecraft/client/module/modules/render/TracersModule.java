package net.minecraft.client.module.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.ColorValue;
import net.minecraft.client.value.EnumValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.List;

public class TracersModule extends Module {
    public enum Mode {
        Bottom,
        Top,
        Center
    }

    private final BooleanValue players = registerValue(new BooleanValue("Players", "Tracers to players", true));
    private final BooleanValue mobs = registerValue(new BooleanValue("Mobs", "Tracers to mobs", false));
    private final BooleanValue animals = registerValue(new BooleanValue("Animals", "Tracers to animals", false));
    private final ColorValue color = registerValue(new ColorValue("Color", "Tracer color", 0xFF00FF00));
    private final EnumValue<Mode> mode = registerValue(new EnumValue<>("Mode", "Tracer origin", Mode.Bottom, List.of(Mode.values())));

    public TracersModule() {
        super("Tracers", "Draws lines to entities", ModuleCategory.RENDER);
    }

    @Override
    public void onRender(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        List<Entity> entities = player.level().getEntities(player, player.getBoundingBox().inflate(100),
            e -> isValidTarget(e));

        int startX = mc.getWindow().getWidth() / 2;
        int startY = switch (mode.getValue()) {
            case Bottom -> mc.getWindow().getHeight();
            case Top -> 0;
            case Center -> mc.getWindow().getHeight() / 2;
        };

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity) {
                Vec3 screenPos = projectToScreen(entity);
                if (screenPos != null) {
                    int endX = (int) screenPos.x;
                    int endY = (int) screenPos.y;
                    // Would need line drawing support
                    // guiGraphics.drawLine(startX, startY, endX, endY, color.getColor());
                }
            }
        }
    }

    private boolean isValidTarget(Entity entity) {
        if (!entity.isAlive()) return false;
        if (entity instanceof Player) return players.getValue();
        if (isHostile(entity)) return mobs.getValue();
        if (isPassive(entity)) return animals.getValue();
        return false;
    }

    private boolean isHostile(Entity entity) {
        return entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER;
    }

    private boolean isPassive(Entity entity) {
        return entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.CREATURE ||
               entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.AMBIENT ||
               entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.WATER_CREATURE;
    }

    private Vec3 projectToScreen(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 entityPos = entity.getEyePosition();
        Vec3 cameraPos = mc.gameRenderer.mainCamera().getPosition();

        double diffX = entityPos.x - cameraPos.x;
        double diffY = entityPos.y - cameraPos.y;
        double diffZ = entityPos.z - cameraPos.z;

        double yaw = Math.toRadians(mc.player.getYRot());
        double pitch = Math.toRadians(mc.player.getXRot());

        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);

        double x = diffX * cosYaw - diffZ * sinYaw;
        double z = diffX * sinYaw + diffZ * cosYaw;
        double y = diffY;

        double fov = Math.toRadians(70);
        double scale = mc.getWindow().getWidth() / (2 * Math.tan(fov / 2));

        if (z <= 0) return null;

        double screenX = mc.getWindow().getWidth() / 2 + (x / z) * scale;
        double screenY = mc.getWindow().getHeight() / 2 - (y / z) * scale;

        return new Vec3(screenX, screenY, z);
    }
}