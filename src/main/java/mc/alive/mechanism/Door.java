package mc.alive.mechanism;

import mc.alive.util.LocationFactory;
import org.bukkit.Location;
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
    private boolean closed = true;

    public Door(Location start, BlockFace face, int id) {
        this.id = id;
        this.start = start;
        this.face = face;
        LocationFactory.setOpen2x2Door(start, face, true);
    }

    public void open() {
        LocationFactory.setOpen2x2Door(start, face, false);
        closed = false;
    }

    public BlockFace getFace() {
        return face;
    }

    public void action(Player p) {
        Optional<ItemStack> key = Arrays.stream(p.getInventory().getContents()).filter(it -> {
            if (it == null || !it.hasItemMeta()) return false;
            var am = it.getItemMeta().getAttributeModifiers(Attribute.GENERIC_LUCK);
            if (am == null) return false;
            var data = am.stream().filter(attributeModifier -> attributeModifier.getKey().equals(key_id)).findFirst();
            return data.isPresent() && data.get().getAmount() == id;
        }).findFirst();
        if (key.isPresent()) {
            LocationFactory.setOpen2x2Door(start, face, !closed);
            closed = !closed;
        } else {
            p.sendMessage(rMsg("无卡"));
        }
    }
}
