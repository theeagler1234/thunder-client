package net.minecraft.client.module.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.FloatValue;
import net.minecraft.client.value.StringValue;
import net.minecraft.network.chat.Component;

public class AutoReconnectModule extends Module {
    private final FloatValue delay = registerValue(new FloatValue("Delay", "Reconnect delay (seconds)", 5.0f, 1.0f, 60.0f, 1.0f));
    private final BooleanValue chat = registerValue(new BooleanValue("Chat", "Send chat message on reconnect", false));
    private final StringValue message = registerValue(new StringValue("Message", "Reconnect message", "Reconnected!", 100));

    private long lastDisconnect = 0;
    private boolean wasConnected = false;

    public AutoReconnectModule() {
        super("AutoReconnect", "Automatically reconnects on disconnect", ModuleCategory.MISC);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.getConnection() != null && mc.getConnection().isConnected()) {
            wasConnected = true;
            return;
        }

        if (wasConnected && mc.getConnection() == null) {
            lastDisconnect = System.currentTimeMillis();
            wasConnected = false;
        }

        if (!wasConnected && lastDisconnect > 0) {
            long elapsed = System.currentTimeMillis() - lastDisconnect;
            if (elapsed >= delay.getValue() * 1000) {
                // Trigger reconnect
                mc.reconnect();
                if (chat.getValue() && mc.player != null) {
                    mc.player.sendChatMessage(message.getValue());
                }
                lastDisconnect = 0;
            }
        }
    }
}