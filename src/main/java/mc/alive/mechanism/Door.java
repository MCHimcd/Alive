package mc.alive.mechanism;

import mc.alive.Game;
import mc.alive.util.LocationFactory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
        LocationFactory.replace2x2Door(start, face, Material.OBSIDIAN);
    }

    public BlockFace getFace() {
        return face;
    }

    public void tryOpen(Player p) {
        Optional<ItemStack> key = Arrays.stream(p.getInventory().getContents()).filter(it -> {
            if (it == null || !it.hasItemMeta()) return false;
            var am = it.getItemMeta().getAttributeModifiers(Attribute.GENERIC_LUCK);
            if (am == null) return false;
            var data = am.stream().filter(attributeModifier -> attributeModifier.getKey().equals(key_id)).findFirst();
            return data.isPresent() && data.get().getAmount() == id;
        }).findFirst();
        if (key.isPresent()) {
            p.getInventory().removeItem(key.get());
            LocationFactory.replace2x2Door(start, face, Material.AIR);
            Game.game.doors.remove(start);
        } else {
            p.sendMessage(rMsg("无卡"));
        }
    }
}
