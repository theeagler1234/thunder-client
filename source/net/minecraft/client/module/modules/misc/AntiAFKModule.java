package net.minecraft.client.module.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.EnumValue;
import net.minecraft.client.value.FloatValue;
import net.minecraft.world.entity.player.Player;
import java.util.List;

public class AntiAFKModule extends Module {
    public enum Mode {
        Jump,
        Sneak,
        Spin,
        Walk,
        Random
    }

    private final EnumValue<Mode> mode = registerValue(new EnumValue<>("Mode", "AntiAFK mode", Mode.Jump, List.of(Mode.values())));
    private final FloatValue interval = registerValue(new FloatValue("Interval", "Action interval (seconds)", 30.0f, 5.0f, 300.0f, 5.0f));
    private final BooleanValue randomize = registerValue(new BooleanValue("Randomize", "Randomize interval", true));
    private final FloatValue randomRange = registerValue(new FloatValue("RandomRange", "Randomization range (%)", 20.0f, 0.0f, 100.0f, 5.0f));
    private final BooleanValue chatMessage = registerValue(new BooleanValue("ChatMessage", "Send chat message", false));
    private final StringValue message = registerValue(new StringValue("Message", "AFK message", "I'm not AFK!", 100));
    private final BooleanValue onlyWhenTabbed = registerValue(new BooleanValue("OnlyWhenTabbed", "Only when tabbed out", false));

    private int tickCounter = 0;
    private int currentInterval = 0;

    public AntiAFKModule() {
        super("AntiAFK", "Prevents AFK kick", ModuleCategory.MISC);
    }

    @Override
    public void onEnable() {
        tickCounter = 0;
        updateInterval();
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        if (onlyWhenTabbed.getValue() && mc.getWindow().isFocused()) return;

        tickCounter++;
        if (tickCounter >= currentInterval) {
            tickCounter = 0;
            updateInterval();
            performAction(player);
        }
    }

    private void updateInterval() {
        float base = interval.getValue() * 20;
        if (randomize.getValue()) {
            float range = base * (randomRange.getValue() / 100.0f);
            currentInterval = (int) (base + (Math.random() - 0.5) * 2 * range);
        } else {
            currentInterval = (int) base;
        }
    }

    private void performAction(Player player) {
        switch (mode.getValue()) {
            case Jump -> player.jumpFromGround();
            case Sneak -> {
                player.setShiftKeyDown(true);
                // Release next tick
            }
            case Spin -> {
                player.setYRot(player.getYRot() + 180);
            }
            case Walk -> {
                // Move slightly
                player.setDeltaMovement(0.01, player.getDeltaMovement().y, 0.01);
            }
            case Random -> {
                int action = (int) (Math.random() * 4);
                switch (action) {
                    case 0 -> player.jumpFromGround();
                    case 1 -> player.setShiftKeyDown(true);
                    case 2 -> player.setYRot(player.getYRot() + (float) (Math.random() * 360));
                    case 3 -> player.setDeltaMovement(0.01, player.getDeltaMovement().y, 0.01);
                }
            }
        }

        if (chatMessage.getValue() && player.getCommandSenderWorld() != null) {
            player.sendChatMessage(message.getValue());
        }
    }
}