package net.minecraft.client.module.modules.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.world.level.Level;

public class NoWeatherModule extends Module {
    private final BooleanValue rain = registerValue(new BooleanValue("Rain", "Disable rain", true));
    private final BooleanValue thunder = registerValue(new BooleanValue("Thunder", "Disable thunder", true));
    private final BooleanValue snow = registerValue(new BooleanValue("Snow", "Disable snow", true));
    private final BooleanValue particles = registerValue(new BooleanValue("Particles", "Disable weather particles", true));
    private final BooleanValue sound = registerValue(new BooleanValue("Sound", "Disable weather sounds", true));
    private final BooleanValue darken = registerValue(new BooleanValue("Darken", "Disable sky darkening", true));

    public NoWeatherModule() {
        super("NoWeather", "Disables rain, thunder, and snow", ModuleCategory.WORLD);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;

        if (level == null) return;

        if (rain.getValue() && level.isRaining()) {
            level.setRainLevel(0.0f);
        }
        if (thunder.getValue() && level.isThundering()) {
            level.setThunderLevel(0.0f);
        }
    }

    @Override
    public void onDisable() {
        // Weather will naturally return
    }
}