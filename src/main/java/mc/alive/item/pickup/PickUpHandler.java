package mc.alive.item.pickup;

import org.bukkit.entity.Player;

public interface PickUpHandler {
    /**
     * @param player 拾取物品的玩家
     * @return 是非删除物品
     */
    boolean handlePickUp(Player player);
}
