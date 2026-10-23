package net.minecraft.client.module.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.EnumValue;
import net.minecraft.client.value.FloatValue;
import net.minecraft.client.renderer.LightTexture;
import java.util.List;

public class FullbrightModule extends Module {
    public enum Mode {
        Gamma,
        Shader,
        NightVision
    }

    private final EnumValue<Mode> mode = registerValue(new EnumValue<>("Mode", "Fullbright mode", Mode.Gamma, List.of(Mode.values())));
    private final FloatValue gamma = registerValue(new FloatValue("Gamma", "Gamma value", 10000.0f, 1.0f, 10000.0f, 100.0f));
    private final BooleanValue nightVision = registerValue(new BooleanValue("NightVision", "Apply night vision effect", false));

    private float previousGamma;

    public FullbrightModule() {
        super("Fullbright", "Removes darkness", ModuleCategory.RENDER);
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        previousGamma = mc.options.gamma().get();

        switch (mode.getValue()) {
            case Gamma -> mc.options.gamma().set(gamma.getValue());
            case Shader -> {
                // Shader-based fullbright would go here
            }
            case NightVision -> {
                if (nightVision.getValue() && mc.player != null) {
                    mc.player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.NIGHT_VISION, 999999, 0, false, false));
                }
            }
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        mc.options.gamma().set(previousGamma);

        if (mode.getValue() == Mode.NightVision && mc.player != null) {
            mc.player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mode.getValue() == Mode.Gamma && mc.options.gamma().get() < 1000.0f) {
            mc.options.gamma().set(gamma.getValue());
        }
    }
}