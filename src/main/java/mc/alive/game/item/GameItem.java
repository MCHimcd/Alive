package mc.alive.game.item;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;

public abstract class GameItem {
    public abstract int customModelData();

    public abstract Component name();

    public abstract List<Component> lore();

    public abstract Material material();

    public abstract PickUp pickUp();


}

