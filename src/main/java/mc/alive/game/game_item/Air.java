package mc.alive.game.game_item;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;

public class Air extends GameItem {
    @Override
    public int customModelData() {
        return 0;
    }

    @Override
    public Component name() {
        return null;
    }

    @Override
    public List<Component> lore() {
        return List.of();
    }

    @Override
    public Material material() {
        return Material.AIR;
    }
}
