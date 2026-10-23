package net.minecraft.client.module.modules.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.FloatValue;
import net.minecraft.world.entity.player.Player;

public class StepModule extends Module {
    private final FloatValue height = registerValue(new FloatValue("Height", "Step height", 1.0f, 0.5f, 2.5f, 0.1f));
    private final BooleanValue sprint = registerValue(new BooleanValue("Sprint", "Step while sprinting", true));

    public StepModule() {
        super("Step", "Automatically steps up blocks", ModuleCategory.PLAYER);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        if (!sprint.getValue() && player.isSprinting()) return;

        if (player.horizontalCollision && player.onGround()) {
            double stepHeight = height.getValue();
            if (player.getDeltaMovement().y <= 0) {
                player.setDeltaMovement(player.getDeltaMovement().x, stepHeight * 0.1, player.getDeltaMovement().z);
            }
        }
    }
}