package net.minecraft.client.module.modules.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.EnumValue;
import net.minecraft.client.value.FloatValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.NonNullList;
import java.util.List;

public class AutoMLGModule extends Module {
    public enum Mode {
        Water,
        Boat,
        Slime,
        Hay,
        PowderSnow
    }

    private final EnumValue<Mode> mode = registerValue(new EnumValue<>("Mode", "MLG mode", Mode.Water, List.of(Mode.values())));
    private final FloatValue height = registerValue(new FloatValue("Height", "Minimum fall height", 10.0f, 3.0f, 100.0f, 1.0f));
    private final BooleanValue silent = registerValue(new BooleanValue("Silent", "Silent place", true));

    public AutoMLGModule() {
        super("AutoMLG", "Automatically places water/boat for fall damage", ModuleCategory.WORLD);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;
        if (player.fallDistance < height.getValue()) return;
        if (player.isOnGround()) return;

        switch (mode.getValue()) {
            case Water -> placeWater(player);
            case Boat -> placeBoat(player);
            case Slime -> placeSlime(player);
            case Hay -> placeHay(player);
            case PowderSnow -> placePowderSnow(player);
        }
    }

    private void placeWater(Player player) {
        ItemStack waterBucket = findItem(player, Items.WATER_BUCKET);
        if (waterBucket == null) return;

        int slot = findSlot(player, Items.WATER_BUCKET);
        if (slot == -1) return;

        BlockPos placePos = getPlacePosBelow(player);
        if (Minecraft.getInstance().gameMode != null) {
            int oldSlot = player.getInventory().selected;
            if (silent.getValue()) {
                // Silent swap
            } else {
                player.getInventory().selected = slot;
            }
            Minecraft.getInstance().gameMode.useItemOn(player, InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(
                    net.minecraft.world.phys.Vec3.atCenterOf(placePos),
                    net.minecraft.core.Direction.UP,
                    placePos,
                    false
                ));
            if (!silent.getValue()) {
                player.getInventory().selected = oldSlot;
            }
        }
    }

    private void placeBoat(Player player) {
        ItemStack boat = findItem(player, Items.OAK_BOAT);
        if (boat == null) return;

        int slot = findSlot(player, Items.OAK_BOAT);
        if (slot == -1) return;

        BlockPos placePos = getPlacePosBelow(player);
        if (Minecraft.getInstance().gameMode != null) {
            int oldSlot = player.getInventory().selected;
            if (silent.getValue()) {
            } else {
                player.getInventory().selected = slot;
            }
            Minecraft.getInstance().gameMode.useItemOn(player, InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(
                    net.minecraft.world.phys.Vec3.atCenterOf(placePos),
                    net.minecraft.core.Direction.UP,
                    placePos,
                    false
                ));
            if (!silent.getValue()) {
                player.getInventory().selected = oldSlot;
            }
        }
    }

    private void placeSlime(Player player) {
        placeBlock(player, Items.SLIME_BLOCK);
    }

    private void placeHay(Player player) {
        placeBlock(player, Items.HAY_BLOCK);
    }

    private void placePowderSnow(Player player) {
        ItemStack bucket = findItem(player, Items.POWDER_SNOW_BUCKET);
        if (bucket == null) return;
        placeWater(player); // Same logic
    }

    private void placeBlock(Player player, net.minecraft.world.item.Item blockItem) {
        ItemStack stack = findItem(player, blockItem);
        if (stack == null) return;

        int slot = findSlot(player, blockItem);
        if (slot == -1) return;

        BlockPos placePos = getPlacePosBelow(player);
        if (Minecraft.getInstance().gameMode != null) {
            int oldSlot = player.getInventory().selected;
            if (silent.getValue()) {
            } else {
                player.getInventory().selected = slot;
            }
            Minecraft.getInstance().gameMode.useItemOn(player, InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(
                    net.minecraft.world.phys.Vec3.atCenterOf(placePos),
                    net.minecraft.core.Direction.UP,
                    placePos,
                    false
                ));
            if (!silent.getValue()) {
                player.getInventory().selected = oldSlot;
            }
        }
    }

    private ItemStack findItem(Player player, net.minecraft.world.item.Item item) {
        NonNullList<ItemStack> inventory = player.getInventory().items;
        for (ItemStack stack : inventory) {
            if (stack.getItem() == item) return stack;
        }
        return null;
    }

    private int findSlot(Player player, net.minecraft.world.item.Item item) {
        NonNullList<ItemStack> inventory = player.getInventory().items;
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).getItem() == item) return i;
        }
        return -1;
    }

    private BlockPos getPlacePosBelow(Player player) {
        return player.getOnPos().below();
    }
}