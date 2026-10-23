package net.minecraft.client.module.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.FloatValue;
import net.minecraft.client.value.IntegerValue;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoTotemModule extends Module {
    private final BooleanValue onlyInCombat = registerValue(new BooleanValue("OnlyInCombat", "Only swap in combat", true));
    private final FloatValue healthThreshold = registerValue(new FloatValue("HealthThreshold", "Health threshold to swap", 8.0f, 1.0f, 20.0f, 0.5f));
    private final BooleanValue offhand = registerValue(new BooleanValue("Offhand", "Swap to offhand", true));
    private final BooleanValue inventory = registerValue(new BooleanValue("Inventory", "Move from inventory to offhand", true));
    private final BooleanValue hotbar = registerValue(new BooleanValue("Hotbar", "Prefer hotbar totems", true));
    private final IntegerValue swapDelay = registerValue(new IntegerValue("SwapDelay", "Delay between swaps (ticks)", 2, 0, 20));
    private final BooleanValue silent = registerValue(new BooleanValue("Silent", "Silent switch", true));
    private final BooleanValue autoSwitchBack = registerValue(new BooleanValue("AutoSwitchBack", "Switch back after combat", true));

    private boolean wasInCombat = false;
    private int swapCooldown = 0;
    private int lastOffhandSlot = -1;

    public AutoTotemModule() {
        super("AutoTotem", "Automatically swaps totem to offhand", ModuleCategory.COMBAT);
    }

    @Override
    public void onEnable() {
        swapCooldown = 0;
        lastOffhandSlot = -1;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        if (swapCooldown > 0) {
            swapCooldown--;
            return;
        }

        boolean inCombat = isInCombat(player);
        boolean shouldSwap = !onlyInCombat.getValue() || inCombat;
        boolean lowHealth = player.getHealth() <= healthThreshold.getValue();

        if (shouldSwap && (lowHealth || inCombat)) {
            ensureTotemInOffhand(player);
        } else if (autoSwitchBack.getValue() && wasInCombat && !inCombat && lastOffhandSlot != -1) {
            switchBack(player);
        }

        wasInCombat = inCombat;
    }

    private boolean isInCombat(Player player) {
        return player.hurtTime > 0 ||
               player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(16), p -> p != player && p.isAlive()).size() > 0;
    }

    private void ensureTotemInOffhand(Player player) {
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() == Items.TOTEM_OF_UNDYING) return;

        if (inventory.getValue()) {
            NonNullList<ItemStack> inventoryItems = player.getInventory().items;

            // Check hotbar first if enabled
            int start = hotbar.getValue() ? 0 : 9;
            int end = hotbar.getValue() ? 9 : inventoryItems.size();

            for (int i = start; i < end; i++) {
                ItemStack stack = inventoryItems.get(i);
                if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                    if (offhand.isEmpty()) {
                        if (silent.getValue()) {
                            // Silent swap
                        } else {
                            lastOffhandSlot = player.getInventory().selected;
                            player.getInventory().setItem(i, offhand);
                            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, stack);
                        }
                    } else if (offhand.getItem() != Items.TOTEM_OF_UNDYING) {
                        for (int j = 0; j < inventoryItems.size(); j++) {
                            if (inventoryItems.get(j).isEmpty() && j != i) {
                                inventoryItems.set(j, offhand);
                                if (!silent.getValue()) {
                                    lastOffhandSlot = player.getInventory().selected;
                                }
                                player.getInventory().setItem(i, offhand);
                                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, stack);
                                break;
                            }
                        }
                    }
                    swapCooldown = swapDelay.getValue();
                    break;
                }
            }
        }
    }

    private void switchBack(Player player) {
        if (lastOffhandSlot != -1 && lastOffhandSlot < player.getInventory().items.size()) {
            ItemStack offhand = player.getOffhandItem();
            if (offhand.getItem() == Items.TOTEM_OF_UNDYING && offhand.getCount() > 1) {
                // Keep totem if multiple
                return;
            }
            player.getInventory().selected = lastOffhandSlot;
            lastOffhandSlot = -1;
        }
    }
}