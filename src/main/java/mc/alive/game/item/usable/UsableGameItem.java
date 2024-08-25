package mc.alive.game.item.usable;

import mc.alive.game.item.GameItem;
import org.bukkit.entity.Player;

public abstract class UsableGameItem extends GameItem {
    public abstract void handleItemUse(Player player);
}
