package mc.alive.util;

import org.bukkit.inventory.ItemStack;

public final class ItemCheck {
    public static boolean hasCustomModelData(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().hasCustomModelData();
    }

    public static boolean isGun(int data) {
        return data >= 80000 && data < 80100;
    }

    public static boolean isUsable(int data) {
        return data >= 80000 && data < 85000;
    }

    public static boolean isSkill(int data) {
        return data >= 10000 && data < 20000;
    }

    public static boolean isGameItem(int data) {
        return data >= 80000 && data < 100000;
    }
}
