package mc.alive.game.item;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GameItem {
    public static final Map<String, Class<? extends GameItem>> registries = new HashMap<>();

    public abstract int customModelData();

    public abstract Component name();

    public abstract List<Component> lore();

    public abstract Material material();

    public abstract PickUp pickUp();

}

