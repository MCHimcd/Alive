package mc.alive.util;

import org.bukkit.inventory.ItemStack;

public final class ItemCheck {
    public static boolean hasCustomModelData(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().hasCustomModelData();
    }

    public static boolean isGun(int data) {
        return data >= 80000 && data < 90000;
    }

    public static boolean isSkill(int data) {
        return data >= 10000 && data < 20000;
    }

    public static boolean isPickable(int data) {
        return data >= 80000 && data < 100000;
    }
}