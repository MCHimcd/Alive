package mc.alive.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ItemCreator {
    private final ItemStack item;
    private ItemCreator(Material type) {
        item = new ItemStack(type);
        item.editMeta(meta -> {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        });
    }
    public static ItemCreator create(Material type) {
        return new ItemCreator(type);
    }
    public ItemCreator hideAttributes() {
        item.editMeta(meta -> meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES));
        return this;
    }
    public ItemStack getItem() {
        return item;
    }
    public ItemCreator name(Component name) {
        item.editMeta(meta -> meta.displayName(name));
        return this;
    }
    public ItemCreator amount(int amount) {
        item.setAmount(amount);
        return this;
    }
    public ItemCreator data(int data) {
        item.editMeta(meta -> meta.setCustomModelData(data));
        return this;
    }
    public ItemCreator lore(Component... lore) {
        item.editMeta(meta -> meta.lore(List.of(lore)));
        return this;
    }
}