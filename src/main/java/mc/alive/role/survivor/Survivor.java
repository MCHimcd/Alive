package mc.alive.role.survivor;

import mc.alive.role.Role;
import org.bukkit.entity.Player;


abstract public class Survivor extends Role {
    public Survivor(Player pl) {
        super(pl);
    }

    /**
     * @return 最大护盾
     */
    abstract public int getMaxShield();

    /**
     * @return 维修速度
     */
    abstract public int getFixSpeed();

}
