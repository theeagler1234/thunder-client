package net.minecraft.client.module.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.ColorValue;
import net.minecraft.client.value.EnumValue;
import net.minecraft.world.entity.player.Player;
import java.util.List;

public class CoordsModule extends Module {
    public enum Mode {
        Full,
        Compact,
        XYZ
    }

    private final EnumValue<Mode> mode = registerValue(new EnumValue<>("Mode", "Display mode", Mode.Full, List.of(Mode.values())));
    private final ColorValue color = registerValue(new ColorValue("Color", "Text color", 0xFFFFFFFF));
    private final BooleanValue showBiome = registerValue(new BooleanValue("ShowBiome", "Show biome", true));
    private final BooleanValue showDimension = registerValue(new BooleanValue("ShowDimension", "Show dimension", true));
    private final BooleanValue showFPS = registerValue(new BooleanValue("ShowFPS", "Show FPS", true));

    public CoordsModule() {
        super("Coords", "Shows coordinates on screen", ModuleCategory.MISC);
    }

    @Override
    public void onRender(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        String text = formatCoords(player);
        int x = 5;
        int y = 5;
        int textColor = color.getColor();

        guiGraphics.drawString(mc.font, text, x, y, textColor, true);

        if (showFPS.getValue()) {
            String fpsText = mc.getFps() + " FPS";
            guiGraphics.drawString(mc.font, fpsText, x, y + 12, textColor, true);
        }
    }

    private String formatCoords(Player player) {
        int x = (int) Math.floor(player.getX());
        int y = (int) Math.floor(player.getY());
        int z = (int) Math.floor(player.getZ());

        StringBuilder sb = new StringBuilder();

        switch (mode.getValue()) {
            case Full -> {
                sb.append("XYZ: ").append(x).append(" ").append(y).append(" ").append(z);
                if (showDimension.getValue()) {
                    sb.append(" | Dim: ").append(player.level().dimension().location().getPath());
                }
                if (showBiome.getValue()) {
                    sb.append(" | Biome: ").append(player.level().getBiome(player.blockPosition()).value().getCategory().name());
                }
            }
            case Compact -> {
                sb.append(x).append(" ").append(y).append(" ").append(z);
            }
            case XYZ -> {
                sb.append("X: ").append(x).append(" Y: ").append(y).append(" Z: ").append(z);
            }
        }

        return sb.toString();
    }
}