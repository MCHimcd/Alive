package mc.alive.role.survivor;

import mc.alive.role.Role;
import org.bukkit.entity.Player;

abstract public class Survivor extends Role {
    public Survivor(Player pl) {
        super(pl);
    }

    //护盾 （仅船员）
    abstract public int getMaxShield();

}
