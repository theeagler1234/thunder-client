package net.minecraft.client.module.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

public class NoSlowModule extends Module {
    private final BooleanValue soulSand = registerValue(new BooleanValue("SoulSand", "No slow on soul sand", true));
    private final BooleanValue honey = registerValue(new BooleanValue("Honey", "No slow on honey", true));
    private final BooleanValue eating = registerValue(new BooleanValue("Eating", "No slow while eating", true));
    private final BooleanValue drinking = registerValue(new BooleanValue("Drinking", "No slow while drinking", true));
    private final BooleanValue bow = registerValue(new BooleanValue("Bow", "No slow while drawing bow", true));
    private final BooleanValue blocking = registerValue(new BooleanValue("Blocking", "No slow while blocking", true));

    public NoSlowModule() {
        super("NoSlow", "Prevents slowing down from items", ModuleCategory.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        if (eating.getValue() && player.isEating()) {
            // Override eating speed penalty
            player.getAttributes().getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                .setBaseValue(0.1f);
        }

        if (drinking.getValue() && player.isDrinking()) {
            // Similar for drinking
        }

        if (bow.getValue()) {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.getItem() == Items.BOW && player.isUsingItem()) {
                // Prevent bow slowdown
            }
        }

        if (blocking.getValue() && player.isBlocking()) {
            // Prevent shield slowdown
        }
    }
}