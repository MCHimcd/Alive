package mc.alive.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

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

    public static ItemStack getGunItemStack(int data) {
        return material(Material.BOW, data).name(switch (data) {
            case 80000 -> text("手枪");
            case 80001 -> text("霰弹枪");
            case 80002 -> text("冲锋枪");
            default -> empty();
        }).build();
    }

    public static ItemBuilder material(Material type, int data) {
        return new ItemBuilder(type).data(data);
    }

    public ItemBuilder data(int data) {
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