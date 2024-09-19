package mc.alive.mechanism;

import mc.alive.util.LocationFactory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.Optional;

import static mc.alive.util.Message.rMsg;

public class Door {
    public static final NamespacedKey key_id = new NamespacedKey("alive", "key_id");
    private final int id;
    private final Block block;

    public Door(Block block, int id) {
        this.id = id;
        this.block = block;
        LocationFactory.replace2x2(block.getLocation(), Material.BARRIER, ((Directional) block.getBlockData()).getFacing());
    }

    public void tryOpen(Player p) {
        Optional<ItemStack> key = Arrays.stream(p.getInventory().getContents()).filter(it -> {
            if (it == null || !it.hasItemMeta()) return false;
            var data = it.getItemMeta().getPersistentDataContainer().get(key_id, PersistentDataType.INTEGER);
            return data != null && data == id;
        }).findFirst();
        if (key.isPresent()) {
            p.getInventory().removeItem(key.get());
            LocationFactory.replace2x2(block.getLocation(), Material.AIR, ((Directional) block.getBlockData()).getFacing());
        } else {
            p.sendMessage(rMsg("无卡"));
        }
    }
}
