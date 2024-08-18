package mc.alive.game.role.survivor;

import mc.alive.game.role.Role;
import org.bukkit.entity.Player;


abstract public class Survivor extends Role {
    public Survivor(Player pl) {
        super(pl);
    }

    //护盾
    abstract public int getMaxShield();

}
