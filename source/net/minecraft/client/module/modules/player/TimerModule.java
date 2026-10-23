package net.minecraft.client.module.modules.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.FloatValue;

public class TimerModule extends Module {
    private final FloatValue speed = registerValue(new FloatValue("Speed", "Game speed multiplier", 1.0f, 0.1f, 5.0f, 0.1f));

    private float previousTimer;

    public TimerModule() {
        super("Timer", "Changes game speed", ModuleCategory.PLAYER);
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        previousTimer = mc.timer;
        mc.timer = speed.getValue();
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        mc.timer = previousTimer;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        mc.timer = speed.getValue();
    }
}