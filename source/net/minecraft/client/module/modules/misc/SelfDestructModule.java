package net.minecraft.client.module.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.EnumValue;
import net.minecraft.network.chat.Component;
import java.util.List;

public class SelfDestructModule extends Module {
    public enum Mode {
        UnloadAll,
        UnloadExceptSelected,
        DisableOnly
    }

    private final EnumValue<Mode> mode = registerValue(new EnumValue<>("Mode", "Self-destruct mode", Mode.UnloadAll, List.of(Mode.values())));
    private final BooleanValue confirm = registerValue(new BooleanValue("Confirm", "Require confirmation", true));
    private final BooleanValue saveConfig = registerValue(new BooleanValue("SaveConfig", "Save config before unload", true));
    private final BooleanValue notifyChat = registerValue(new BooleanValue("NotifyChat", "Send chat notification", false));
    private final StringValue message = registerValue(new StringValue("Message", "Unload message", "Self-destruct initiated", 100));

    private boolean confirmationPending = false;

    public SelfDestructModule() {
        super("SelfDestruct", "Unloads all modules", ModuleCategory.MISC);
    }

    @Override
    public void onEnable() {
        if (confirm.getValue() && !confirmationPending) {
            confirmationPending = true;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("§cPress the key again to confirm self-destruct"));
            }
            setEnabled(false);
            return;
        }

        if (saveConfig.getValue()) {
            ModuleManager.getInstance().saveConfig();
        }

        if (notifyChat.getValue()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendChatMessage(message.getValue());
            }
        }

        switch (mode.getValue()) {
            case UnloadAll -> {
                ModuleManager.getInstance().getModules().forEach(module -> {
                    module.setEnabled(false);
                });
            }
            case UnloadExceptSelected -> {
                ModuleManager.getInstance().getModules().forEach(module -> {
                    if (module != this) {
                        module.setEnabled(false);
                    }
                });
            }
            case DisableOnly -> {
                ModuleManager.getInstance().getModules().forEach(module -> {
                    if (module != this) {
                        module.setEnabled(false);
                    }
                });
            }
        }

        confirmationPending = false;
        setEnabled(false);
    }

    @Override
    public void onDisable() {
        if (!confirmationPending) {
            // Reset confirmation state when disabled externally
            confirmationPending = false;
        }
    }
}