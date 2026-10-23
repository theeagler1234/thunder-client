package net.minecraft.client.module.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.world.entity.player.Player;

public class NoRenderModule extends Module {
    private final BooleanValue fog = registerValue(new BooleanValue("Fog", "Disable fog", true));
    private final BooleanValue fire = registerValue(new BooleanValue("Fire", "Disable fire overlay", true));
    private final BooleanValue pumpkin = registerValue(new BooleanValue("Pumpkin", "Disable pumpkin overlay", true));
    private final BooleanValue portal = registerValue(new BooleanValue("Portal", "Disable portal overlay", true));
    private final BooleanValue hurt = registerValue(new BooleanValue("Hurt", "Disable hurt overlay", false));
    private final BooleanValue vignette = registerValue(new BooleanValue("Vignette", "Disable vignette", false));
    private final BooleanValue darkScreen = registerValue(new BooleanValue("DarkScreen", "Disable dark screen effect", true));

    public NoRenderModule() {
        super("NoRender", "Disables specific rendering effects", ModuleCategory.RENDER);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        if (fog.getValue()) {
            // Fog is handled by the renderer - would need to hook into render pipeline
        }

        if (fire.getValue() && player.isOnFire()) {
            player.clearFire();
        }

        if (hurt.getValue() && player.hurtTime > 0) {
            player.hurtTime = 0;
        }
    }
}