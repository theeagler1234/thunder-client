package net.minecraft.client.module.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.EnumValue;
import net.minecraft.client.value.StringValue;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import java.util.List;

public class ChatSpyModule extends Module {
    public enum LogMode {
        Chat,
        File,
        Both
    }

    private final BooleanValue commands = registerValue(new BooleanValue("Commands", "Log commands", true));
    private final BooleanValue privateMessages = registerValue(new BooleanValue("PrivateMessages", "Log private messages", true));
    private final EnumValue<LogMode> logMode = registerValue(new EnumValue<>("LogMode", "Log output mode", LogMode.Chat, List.of(LogMode.values())));
    private final StringValue logFile = registerValue(new StringValue("LogFile", "Log file name", "chatspy.log", 50));
    private final BooleanValue timestamps = registerValue(new BooleanValue("Timestamps", "Add timestamps", true));
    private final BooleanValue highlight = registerValue(new BooleanValue("Highlight", "Highlight spy messages", true));
    private final BooleanValue filterBots = registerValue(new BooleanValue("FilterBots", "Filter bot messages", false));

    public ChatSpyModule() {
        super("ChatSpy", "Shows private messages and commands", ModuleCategory.MISC);
    }

    @Override
    public void onEnable() {
        // Hook into chat packet handling
    }

    @Override
    public void onDisable() {
        // Remove hook
    }

    public void onChatPacket(ClientboundPlayerChatPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        PlayerChatMessage message = packet.message();
        String content = message.content().getString();
        Component sender = message.signedBody() != null ? message.signedBody().name() : Component.literal("Unknown");

        // Filter bot messages if enabled
        if (filterBots.getValue() && isBotMessage(sender.getString())) return;

        String timestamp = "";
        if (timestamps.getValue()) {
            java.time.LocalTime now = java.time.LocalTime.now();
            timestamp = String.format("[%02d:%02d:%02d] ", now.getHour(), now.getMinute(), now.getSecond());
        }

        String spyPrefix = highlight.getValue() ? "§8[§bSpy§8] " : "[Spy] ";
        String pmPrefix = highlight.getValue() ? "§8[§dPM§8] " : "[PM] ";

        if (content.startsWith("/") && commands.getValue()) {
            String logMsg = spyPrefix + timestamp + sender + ": " + content;
            if (logMode.getValue() == LogMode.Chat || logMode.getValue() == LogMode.Both) {
                mc.gui.getChat().addMessage(Component.literal(logMsg).withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            if (logMode.getValue() == LogMode.File || logMode.getValue() == LogMode.Both) {
                writeToFile(logMsg);
            }
        }

        if (privateMessages.getValue() && isPrivateMessage(content)) {
            String logMsg = pmPrefix + timestamp + sender + ": " + content;
            if (logMode.getValue() == LogMode.Chat || logMode.getValue() == LogMode.Both) {
                mc.gui.getChat().addMessage(Component.literal(logMsg).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
            }
            if (logMode.getValue() == LogMode.File || logMode.getValue() == LogMode.Both) {
                writeToFile(logMsg);
            }
        }
    }

    private boolean isPrivateMessage(String content) {
        return content.contains("->") || content.contains("whispers") || content.contains("tells you") ||
               content.contains("msg") || content.contains("tell") || content.contains("w ");
    }

    private boolean isBotMessage(String sender) {
        String lower = sender.toLowerCase();
        return lower.contains("bot") || lower.contains("npc") || lower.matches(".*\\d{3,}.*");
    }

    private void writeToFile(String message) {
        try {
            java.io.FileWriter writer = new java.io.FileWriter(logFile.getValue(), true);
            writer.write(message + "\n");
            writer.close();
        } catch (java.io.IOException e) {
            // Ignore
        }
    }
}