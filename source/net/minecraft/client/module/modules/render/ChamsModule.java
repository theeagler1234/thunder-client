package net.minecraft.client.module.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.ColorValue;
import net.minecraft.client.value.EnumValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.List;

public class ChamsModule extends Module {
    public enum Mode {
        Solid,
        Wireframe,
        Both
    }

    private final EnumValue<Mode> mode = registerValue(new EnumValue<>("Mode", "Chams mode", Mode.Solid, List.of(Mode.values())));
    private final ColorValue color = registerValue(new ColorValue("Color", "Chams color", 0xFF00FFFF));
    private final BooleanValue throughWalls = registerValue(new BooleanValue("ThroughWalls", "Render through walls", true));
    private final BooleanValue players = registerValue(new BooleanValue("Players", "Chams on players", true));
    private final BooleanValue mobs = registerValue(new BooleanValue("Mobs", "Chams on mobs", false));
    private final BooleanValue animals = registerValue(new BooleanValue("Animals", "Chams on animals", false));
    private final BooleanValue items = registerValue(new BooleanValue("Items", "Chams on items", false));

    public ChamsModule() {
        super("Chams", "Renders entities with custom materials", ModuleCategory.RENDER);
    }

    @Override
    public void onRender(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;

        if (player == null || level == null) return;

        List<Entity> entities = level.getEntities(player, player.getBoundingBox().inflate(100),
            e -> isValidTarget(e));

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                renderChams(guiGraphics, living);
            }
        }
    }

    private boolean isValidTarget(Entity entity) {
        if (!entity.isAlive()) return false;
        if (entity instanceof Player) return players.getValue();
        if (isHostile(entity)) return mobs.getValue();
        if (isPassive(entity)) return animals.getValue();
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity) return items.getValue();
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

    private void renderChams(GuiGraphics guiGraphics, LivingEntity entity) {
        // Chams rendering would require access to the render pipeline
        // This would typically be done by registering a custom render layer
        // or by modifying the entity renderer
    }
}