package net.minecraft.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.clickgui.ClickGUIScreen;
import net.minecraft.client.input.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ModuleManager {
    private static ModuleManager INSTANCE;
    private final List<Module> modules;
    private final List<Consumer<Module>> toggleListeners;

    private ModuleManager() {
        this.modules = new ArrayList<>();
        this.toggleListeners = new ArrayList<>();
    }

    public static ModuleManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ModuleManager();
        }
        return INSTANCE;
    }

    public void registerModule(Module module) {
        modules.add(module);
        module.setOnToggle(this::onModuleToggle);
    }

    public void registerModules(Module... modules) {
        for (Module module : modules) {
            registerModule(module);
        }
    }

    public List<Module> getModules() {
        return List.copyOf(modules);
    }

    public List<Module> getModulesByCategory(ModuleCategory category) {
        List<Module> result = new ArrayList<>();
        for (Module module : modules) {
            if (module.getCategory() == category) {
                result.add(module);
            }
        }
        return result;
    }

    public List<Module> getEnabledModules() {
        List<Module> result = new ArrayList<>();
        for (Module module : modules) {
            if (module.isEnabled()) {
                result.add(module);
            }
        }
        return result;
    }

    public Module getModuleByName(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public void onTick() {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onTick();
            }
        }
    }

    public void onRender() {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onRender();
            }
        }
    }

    public void onKeyPress(int key) {
        for (Module module : modules) {
            if (module.getKeybind() == key && key != 0) {
                module.toggle();
            }
        }
    }

    public void addToggleListener(Consumer<Module> listener) {
        toggleListeners.add(listener);
    }

    public void removeToggleListener(Consumer<Module> listener) {
        toggleListeners.remove(listener);
    }

    private void onModuleToggle(Module module) {
        for (Consumer<Module> listener : toggleListeners) {
            listener.accept(module);
        }
    }

    public void openClickGUI() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.gui.setScreen(new ClickGUIScreen()));
    }
}