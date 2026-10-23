package net.minecraft.client.module.modules.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.FloatValue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.FoodProperties;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.NonNullList;

public class AutoEatModule extends Module {
    private final FloatValue hungerThreshold = registerValue(new FloatValue("HungerThreshold", "Hunger level to eat at", 14.0f, 1.0f, 19.0f, 1.0f));
    private final BooleanValue sprint = registerValue(new BooleanValue("Sprint", "Eat while sprinting", false));
    private final BooleanValue gapple = registerValue(new BooleanValue("Gapple", "Eat golden apples", true));
    private final BooleanValue normalFood = registerValue(new BooleanValue("NormalFood", "Eat normal food", true));
    private final BooleanValue silent = registerValue(new BooleanValue("Silent", "Silent switch", true));

    private int eatCooldown = 0;

    public AutoEatModule() {
        super("AutoEat", "Automatically eats food when hungry", ModuleCategory.PLAYER);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || player.isDeadOrDying()) return;

        if (eatCooldown > 0) {
            eatCooldown--;
            return;
        }

        if (player.getFoodData().getFoodLevel() > hungerThreshold.getValue()) return;

        if (!sprint.getValue() && player.isSprinting()) return;

        if (player.isUsingItem()) return;

        ItemStack foodItem = findBestFood(player);
        if (foodItem == null) return;

        int slot = findFoodSlot(player, foodItem);
        if (slot == -1) return;

        if (silent.getValue()) {
            // Silent switch - would need packet manipulation
        } else {
            player.getInventory().selected = slot;
        }

        mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        eatCooldown = 4; // 4 ticks cooldown
    }

    private ItemStack findBestFood(Player player) {
        NonNullList<ItemStack> inventory = player.getInventory().items;
        ItemStack bestFood = null;
        int bestValue = 0;

        for (ItemStack stack : inventory) {
            if (stack.isEmpty()) continue;
            if (!stack.isEdible()) continue;

            FoodProperties food = stack.getItem().getFoodProperties();
            if (food == null) continue;

            boolean isGapple = stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE || stack.getItem() == Items.GOLDEN_APPLE;
            if (isGapple && !gapple.getValue()) continue;
            if (!isGapple && !normalFood.getValue()) continue;

            int value = food.getNutrition() + food.getSaturationModifier() * 2;
            if (isGapple) value += 100; // Prioritize gapples

            if (value > bestValue) {
                bestValue = value;
                bestFood = stack;
            }
        }

        return bestFood;
    }

    private int findFoodSlot(Player player, ItemStack foodItem) {
        NonNullList<ItemStack> inventory = player.getInventory().items;
        for (int i = 0; i < inventory.size(); i++) {
            if (ItemStack.isSameItemSameComponents(inventory.get(i), foodItem)) {
                return i;
            }
        }
        return -1;
    }
}