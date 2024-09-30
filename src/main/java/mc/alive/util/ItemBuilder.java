package mc.alive.util;

import mc.alive.Alive;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

public final class ItemBuilder {
    private final ItemStack item;

    private ItemBuilder(Material type) {
        item = new ItemStack(type);
        item.editMeta(meta -> {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        });
    }

    public static ItemBuilder material(Material type) {
        return new ItemBuilder(type);
    }

    public static ItemBuilder material(Material type, int data) {
        return new ItemBuilder(type).modelData(data);
    }

    public ItemBuilder modelData(int data) {
        item.editMeta(meta -> meta.setCustomModelData(data));
        return this;
    }

    public ItemStack build() {
        return item;
    }

    public ItemBuilder name(Component name) {
        item.editMeta(meta -> meta.displayName(name));
        return this;
    }

    public ItemBuilder unique() {
        item.editMeta(meta -> {
            meta.addAttributeModifier(Attribute.GENERIC_LUCK, new AttributeModifier(new NamespacedKey(Alive.plugin, String.valueOf(new Random().nextDouble())), 1, AttributeModifier.Operation.ADD_NUMBER));
        });
        return this.hideAttributes();
    }

    public ItemBuilder hideAttributes() {
        item.editMeta(meta -> meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES));
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder lore(List<Component> lore) {
        item.editMeta(meta -> meta.lore(lore));
        return this;
    }

}