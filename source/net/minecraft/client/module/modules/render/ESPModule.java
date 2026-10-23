package net.minecraft.client.module.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.ColorValue;
import net.minecraft.client.value.FloatValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class ESPModule extends Module {
    private final BooleanValue players = registerValue(new BooleanValue("Players", "Show players", true));
    private final BooleanValue mobs = registerValue(new BooleanValue("Mobs", "Show hostile mobs", false));
    private final BooleanValue animals = registerValue(new BooleanValue("Animals", "Show animals", false));
    private final BooleanValue items = registerValue(new BooleanValue("Items", "Show dropped items", false));
    private final BooleanValue chests = registerValue(new BooleanValue("Chests", "Show containers", false));
    private final ColorValue playerColor = registerValue(new ColorValue("PlayerColor", "Player ESP color", 0xFFFF0000));
    private final ColorValue mobColor = registerValue(new ColorValue("MobColor", "Mob ESP color", 0xFFFFFF00));
    private final ColorValue animalColor = registerValue(new ColorValue("AnimalColor", "Animal ESP color", 0xFF00FF00));
    private final BooleanValue box = registerValue(new BooleanValue("Box", "Show 2D box", true));
    private final BooleanValue skeleton = registerValue(new BooleanValue("Skeleton", "Show skeleton", false));
    private final BooleanValue health = registerValue(new BooleanValue("Health", "Show health", true));
    private final BooleanValue name = registerValue(new BooleanValue("Name", "Show name", true));
    private final BooleanValue distance = registerValue(new BooleanValue("Distance", "Show distance", true));
    private final FloatValue maxDistance = registerValue(new FloatValue("MaxDistance", "Maximum render distance", 100.0f, 10.0f, 500.0f, 5.0f));
    private final BooleanValue tracers = registerValue(new BooleanValue("Tracers", "Draw tracers to entities", false));
    private final ColorValue tracerColor = registerValue(new ColorValue("TracerColor", "Tracer color", 0xFF00FF00));

    public ESPModule() {
        super("ESP", "Shows entities through walls", ModuleCategory.RENDER);
    }

    @Override
    public void onRender(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;

        if (player == null || level == null) return;

        List<Entity> entities = level.getEntities(player, player.getBoundingBox().inflate(maxDistance.getValue()),
            e -> isValidTarget(e));

        for (Entity entity : entities) {
            renderEntityESP(guiGraphics, player, entity);
        }
    }

    private boolean isValidTarget(Entity entity) {
        if (!entity.isAlive()) return false;
        if (entity instanceof Player) return players.getValue();
        if (isHostile(entity)) return mobs.getValue();
        if (isPassive(entity)) return animals.getValue();
        if (isItem(entity)) return items.getValue();
        if (isContainer(entity)) return chests.getValue();
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

    private boolean isItem(Entity entity) {
        return entity instanceof net.minecraft.world.entity.item.ItemEntity;
    }

    private boolean isContainer(Entity entity) {
        return entity instanceof net.minecraft.world.entity.DecorationEntity ||
               entity.getType().toString().contains("CHEST") ||
               entity.getType().toString().contains("BARREL") ||
               entity.getType().toString().contains("SHULKER");
    }

    private void renderEntityESP(GuiGraphics guiGraphics, Player player, Entity entity) {
        double distance = player.distanceTo(entity);
        if (distance > maxDistance.getValue()) return;

        Vec3 screenPos = projectToScreen(entity);
        if (screenPos == null) return;

        int x = (int) screenPos.x;
        int y = (int) screenPos.y;
        int color = getEntityColor(entity);

        if (box.getValue() && entity instanceof LivingEntity) {
            renderBox(guiGraphics, x, y, entity.getBbWidth(), entity.getBbHeight(), distance, color);
        }

        if (name.getValue()) {
            String entityName = entity.getName().getString();
            int nameWidth = Minecraft.getInstance().font.width(entityName);
            guiGraphics.drawString(Minecraft.getInstance().font, entityName, x - nameWidth / 2, y - 20, color, true);
        }

        if (health.getValue() && entity instanceof LivingEntity living) {
            String healthStr = String.format("%.1f", living.getHealth());
            int healthWidth = Minecraft.getInstance().font.width(healthStr);
            guiGraphics.drawString(Minecraft.getInstance().font, healthStr, x - healthWidth / 2, y - 10, color, true);
        }

        if (distance.getValue()) {
            String distStr = String.format("%.1fm", distance);
            int distWidth = Minecraft.getInstance().font.width(distStr);
            guiGraphics.drawString(Minecraft.getInstance().font, distStr, x - distWidth / 2, y + 15, color, true);
        }

        if (tracers.getValue()) {
            renderTracer(guiGraphics, x, y, tracerColor.getColor());
        }
    }

    private Vec3 projectToScreen(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null || mc.gameRenderer.mainCamera() == null) return null;

        Vec3 entityPos = entity.getEyePosition();
        Vec3 cameraPos = mc.gameRenderer.mainCamera().getPosition();
        float partialTick = mc.getFrameTime();

        // This is simplified - actual implementation would use the proper projection matrix
        // For now, return a basic projection
        double diffX = entityPos.x - cameraPos.x;
        double diffY = entityPos.y - cameraPos.y;
        double diffZ = entityPos.z - cameraPos.z;

        // Very basic screen projection (would need actual view matrix)
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

    private int getEntityColor(Entity entity) {
        if (entity instanceof Player) return playerColor.getColor();
        if (isHostile(entity)) return mobColor.getColor();
        if (isPassive(entity)) return animalColor.getColor();
        return 0xFFFFFFFF;
    }

    private void renderBox(GuiGraphics guiGraphics, int x, int y, float width, float height, double distance, int color) {
        float scale = (float) (mc.getWindow().getWidth() / (distance * 0.1));
        int boxWidth = Math.max(20, (int) (width * scale * 100));
        int boxHeight = Math.max(30, (int) (height * scale * 100));

        int left = x - boxWidth / 2;
        int top = y - boxHeight / 2;

        guiGraphics.fill(left, top, left + boxWidth, top + 1, color);
        guiGraphics.fill(left, top + boxHeight - 1, left + boxWidth, top + boxHeight, color);
        guiGraphics.fill(left, top, left + 1, top + boxHeight, color);
        guiGraphics.fill(left + boxWidth - 1, top, left + boxWidth, top + boxHeight, color);
    }

    private void renderTracer(GuiGraphics guiGraphics, int x, int y, int color) {
        int screenX = mc.getWindow().getWidth() / 2;
        int screenY = mc.getWindow().getHeight();
        // Would need actual line drawing
    }
}