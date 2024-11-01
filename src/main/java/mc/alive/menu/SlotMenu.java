package mc.alive.menu;

import mc.alive.Alive;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.function.BiConsumer;


public abstract class SlotMenu implements InventoryHolder {
    protected final Player player;
    private final Inventory inventory;
    private final HashMap<Integer, BiConsumer<ItemStack, Player>> slotFunctions = new HashMap<>();
    protected boolean close = true;

    public SlotMenu(int size, Component title, Player p) {
        inventory = Alive.plugin.getServer().createInventory(this, size, title);
        player = p;
    }

    /**
     * 设置槽位物品和点击事件
     * @param slot 槽位
     * @param item 物品
     * @param function 被点击时执行
     */
    public void setSlot(int slot, ItemStack item, BiConsumer<ItemStack, Player> function) {
        inventory.setItem(slot, item);
        slotFunctions.put(slot, function);
    }

    /**
     * 移除槽位物品和点击事件
     * @param slot 槽位
     */
    public void removeSlot(int slot) {
        inventory.setItem(slot, null);
        slotFunctions.remove(slot);
    }

    /**
     * 处理槽位点击事件
     * @param slot 槽位
     */
    public void handleClick(int slot) {
        if (slotFunctions.containsKey(slot)) {
            slotFunctions.get(slot).accept(inventory.getItem(slot), player);
            player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
            if (close) player.closeInventory();
        }
        close = true;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}