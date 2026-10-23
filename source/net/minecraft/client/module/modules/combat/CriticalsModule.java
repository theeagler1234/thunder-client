package net.minecraft.client.module.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.EnumValue;
import net.minecraft.world.entity.player.Player;
import java.util.List;

public class CriticalsModule extends Module {
    public enum Mode {
        Packet,
        Jump,
        Both
    }

    private final EnumValue<Mode> mode = registerValue(new EnumValue<>("Mode", "Critical hit mode", Mode.Packet, List.of(Mode.values())));
    private final BooleanValue swingHand = registerValue(new BooleanValue("SwingHand", "Swing hand on crit", true));
    private final BooleanValue onlyWhenFalling = registerValue(new BooleanValue("OnlyWhenFalling", "Only crit when falling", false));
    private final FloatValue fallDistance = registerValue(new FloatValue("FallDistance", "Minimum fall distance for jump crit", 0.01f, 0.01f, 1.0f, 0.01f));
    private final BooleanValue noHurtCam = registerValue(new BooleanValue("NoHurtCam", "Disable hurt camera shake", false));

    public CriticalsModule() {
        super("Criticals", "Always hit critical hits", ModuleCategory.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || mc.gameMode == null) return;

        if (player.getAttackStrengthScale(0.5f) > 0.9f && !player.isFallFlying()) {
            switch (mode.getValue()) {
                case Packet -> {
                    // Send critical packet - this would require packet manipulation
                    // For now, just ensure jump crits work
                    if (swingHand.getValue()) {
                        player.swing(player.getUsedItemHand());
                    }
                }
                case Jump -> {
                    if (player.onGround() && !player.isSprinting()) {
                        if (!onlyWhenFalling.getValue() || player.fallDistance > fallDistance.getValue()) {
                            player.jumpFromGround();
                        }
                    }
                    if (swingHand.getValue()) {
                        player.swing(player.getUsedItemHand());
                    }
                }
                case Both -> {
                    if (player.onGround() && !player.isSprinting()) {
                        if (!onlyWhenFalling.getValue() || player.fallDistance > fallDistance.getValue()) {
                            player.jumpFromGround();
                        }
                    }
                    // Packet critical would go here
                    if (swingHand.getValue()) {
                        player.swing(player.getUsedItemHand());
                    }
                }
            }
        }
    }
}