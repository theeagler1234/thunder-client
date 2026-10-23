package net.minecraft.client.module;

import net.minecraft.client.module.modules.combat.*;
import net.minecraft.client.module.modules.movement.*;
import net.minecraft.client.module.modules.render.*;
import net.minecraft.client.module.modules.player.*;
import net.minecraft.client.module.modules.world.*;
import net.minecraft.client.module.modules.misc.*;

public class ModuleInitializer {
    public static void init() {
        ModuleManager manager = ModuleManager.getInstance();

        // Combat modules
        manager.registerModules(
            new KillAuraModule(),
            new AutoCrystalModule(),
            new AutoTotemModule(),
            new CriticalsModule()
        );

        // Movement modules
        manager.registerModules(
            new SpeedModule(),
            new FlyModule(),
            new JesusModule(),
            new NoSlowModule(),
            new ScaffoldModule(),
            new AutoWalkModule()
        );

        // Render modules
        manager.registerModules(
            new ESPModule(),
            new FullbrightModule(),
            new NoRenderModule(),
            new ChamsModule(),
            new TracersModule(),
            new FreecamModule()
        );

        // Player modules
        manager.registerModules(
            new FlightModule(),
            new InfiniteReachModule(),
            new AutoEatModule(),
            new AutoSwordModule(),
            new NoFallModule(),
            new StepModule(),
            new TimerModule()
        );

        // World modules
        manager.registerModules(
            new XRayModule(),
            new SeedCrackerModule(),
            new CaveFinderModule(),
            new SafeWalkModule(),
            new AutoMLGModule(),
            new HoleFinderModule(),
            new NoWeatherModule()
        );

        // Misc modules
        manager.registerModules(
            new AutoReconnectModule(),
            new AntiAFKModule(),
            new ChatSpyModule(),
            new CoordsModule(),
            new KeystrokesModule(),
            new SelfDestructModule()
        );
    }
}