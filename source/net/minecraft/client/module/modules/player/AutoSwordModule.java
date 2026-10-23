package net.minecraft.client.module.modules.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.EnumValue;
import net.minecraft.client.value.FloatValue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ToolItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.core.NonNullList;
import java.util.List;

public class AutoSwordModule extends Module {
    public enum WeaponPriority {
        HighestDamage,
        SwordFirst,
        AxeFirst,
        BestTool
    }

    private final BooleanValue tools = registerValue(new BooleanValue("Tools", "Switch for tools", true));
    private final BooleanValue blocks = registerValue(new BooleanValue("Blocks", "Switch for blocks", false));
    private final BooleanValue silent = registerValue(new BooleanValue("Silent", "Silent switch", true));
    private final EnumValue<WeaponPriority> weaponPriority = registerValue(new EnumValue<>("WeaponPriority", "Weapon selection priority", WeaponPriority.HighestDamage, List.of(WeaponPriority.values())));
    private final BooleanValue hotbarOnly = registerValue(new BooleanValue("HotbarOnly", "Only use hotbar slots", false));
    private final FloatValue switchDelay = registerValue(new FloatValue("SwitchDelay", "Delay between switches (ticks)", 4.0f, 0.0f, 20.0f, 1.0f));
    private final BooleanValue autoSwitchBack = registerValue(new BooleanValue("AutoSwitchBack", "Switch back when done", true));
    private final BooleanValue favorNewItems = registerValue(new BooleanValue("FavorNewItems", "Prefer newer/damaged items", false));

    private int lastSlot = -1;
    private int switchCooldown = 0;

    public AutoSwordModule() {
        super("AutoSword", "Automatically switches to best weapon/tool", ModuleCategory.PLAYER);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        if (switchCooldown > 0) {
            switchCooldown--;
            return;
        }

        ItemStack mainHand = player.getMainHandItem();

        if (mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            net.minecraft.world.phys.BlockHitResult blockHit = (net.minecraft.world.phys.BlockHitResult) mc.hitResult;
            net.minecraft.world.level.block.state.BlockState state = mc.level.getBlockState(blockHit.getBlockPos());

            if (tools.getValue() || blocks.getValue()) {
                ItemStack bestTool = findBestTool(player, state);
                if (bestTool != null && !ItemStack.isSameItemSameComponents(mainHand, bestTool)) {
                    int slot = findToolSlot(player, bestTool);
                    if (slot != -1) {
                        lastSlot = player.getInventory().selected;
                        if (silent.getValue()) {
                            // Silent switch
                        } else {
                            player.getInventory().selected = slot;
                        }
                        switchCooldown = (int) switchDelay.getValue();
                    }
                }
            }
        } else if (mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY) {
            ItemStack bestWeapon = findBestWeapon(player);
            if (bestWeapon != null && !ItemStack.isSameItemSameComponents(mainHand, bestWeapon)) {
                int slot = findToolSlot(player, bestWeapon);
                if (slot != -1) {
                    lastSlot = player.getInventory().selected;
                    if (silent.getValue()) {
                        // Silent switch
                    } else {
                        player.getInventory().selected = slot;
                    }
                    switchCooldown = (int) switchDelay.getValue();
                }
            }
        } else if (lastSlot != -1 && player.getInventory().selected != lastSlot) {
            // Switch back when not targeting anything
            if (autoSwitchBack.getValue()) {
                if (silent.getValue()) {
                    // Silent switch
                } else {
                    player.getInventory().selected = lastSlot;
                }
            }
            lastSlot = -1;
        }
    }

    private ItemStack findBestWeapon(Player player) {
        NonNullList<ItemStack> inventory = player.getInventory().items;
        ItemStack bestWeapon = null;
        float bestDamage = 0;

        int start = hotbarOnly.getValue() ? 0 : 0;
        int end = hotbarOnly.getValue() ? 9 : inventory.size();

        for (int i = start; i < end; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isEmpty()) continue;

            float damage = 1.0f; // Default fist damage

            if (stack.getItem() instanceof SwordItem sword) {
                damage = sword.getDamage();
            } else if (stack.getItem() instanceof AxeItem axe) {
                damage = axe.getAttackDamage();
            }

            if (favorNewItems.getValue()) {
                // Slightly favor items with more durability
                int maxDamage = stack.getMaxDamage();
                if (maxDamage > 0) {
                    damage += (1.0f - (float) stack.getDamageValue() / maxDamage) * 0.1f;
                }
            }

            if (damage > bestDamage) {
                bestDamage = damage;
                bestWeapon = stack;
            }
        }

        return bestWeapon;
    }

    private ItemStack findBestTool(Player player, net.minecraft.world.level.block.state.BlockState state) {
        NonNullList<ItemStack> inventory = player.getInventory().items;
        ItemStack bestTool = null;
        float bestSpeed = 1.0f;

        int start = hotbarOnly.getValue() ? 0 : 0;
        int end = hotbarOnly.getValue() ? 9 : inventory.size();

        for (int i = start; i < end; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isEmpty()) continue;

            float speed = stack.getDestroySpeed(state);
            if (favorNewItems.getValue()) {
                int maxDamage = stack.getMaxDamage();
                if (maxDamage > 0) {
                    speed += (1.0f - (float) stack.getDamageValue() / maxDamage) * 0.1f;
                }
            }
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestTool = stack;
            }
        }

        if (blocks.getValue() && bestTool != null) return bestTool;

        if (tools.getValue() && bestTool != null && bestTool.getItem() instanceof ToolItem) {
            return bestTool;
        }

        return null;
    }

    private int findToolSlot(Player player, ItemStack tool) {
        NonNullList<ItemStack> inventory = player.getInventory().items;
        int start = hotbarOnly.getValue() ? 0 : 0;
        int end = hotbarOnly.getValue() ? 9 : inventory.size();
        for (int i = start; i < end; i++) {
            if (ItemStack.isSameItemSameComponents(inventory.get(i), tool)) {
                return i;
            }
        }
        return -1;
    }
}