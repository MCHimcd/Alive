package mc.alive.Menu;

import mc.alive.Alive;
import net.kyori.adventure.text.Component;
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

    public SlotMenu(int size, Component title, Player p) {
        inventory = Alive.instance.getServer().createInventory(this, size, title);
        player = p;
    }

    public void setSlot(int slot, ItemStack item, BiConsumer<ItemStack, Player> function) {
        inventory.setItem(slot, item);
        slotFunctions.put(slot, function);
    }

    public void removeSlot(int slot) {
        inventory.setItem(slot, null);
        slotFunctions.remove(slot);
    }

    public void handleClick(int slot) {
        if (slotFunctions.containsKey(slot)) {
            player.closeInventory();
            slotFunctions.get(slot).accept(inventory.getItem(slot), player);
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}