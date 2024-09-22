package mc.alive.mechanism;

import mc.alive.util.LocationFactory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.Optional;

import static mc.alive.util.Message.rMsg;

public class Door {
    public static final NamespacedKey key_id = new NamespacedKey("alive", "key_id");
    private final int id;
    private final Location start;
    private final BlockFace face;

    public Door(Location start, BlockFace face, int id) {
        this.id = id;
        this.start = start;
        this.face = face;
        LocationFactory.replace2x2Door(start, face, Material.BARRIER);
    }

    public BlockFace getFace() {
        return face;
    }

    public void tryOpen(Player p) {
        Optional<ItemStack> key = Arrays.stream(p.getInventory().getContents()).filter(it -> {
            if (it == null || !it.hasItemMeta()) return false;
            var data = it.getItemMeta().getPersistentDataContainer().get(key_id, PersistentDataType.INTEGER);
            return data != null && data == id;
        }).findFirst();
        if (key.isPresent()) {
            p.getInventory().removeItem(key.get());
            LocationFactory.replace2x2Door(start, face, Material.AIR);
        } else {
            p.sendMessage(rMsg("无卡"));
        }
    }
}
