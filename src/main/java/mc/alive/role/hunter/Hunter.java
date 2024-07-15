package mc.alive.role.hunter;

import mc.alive.role.Role;
import org.bukkit.entity.Player;

abstract public class Hunter extends Role {
    public Hunter(Player pl) {
        super(pl);
    }

    //最大等级
    abstract public int getMaxLevel();
    abstract public double getRange();

    public void levelUp(){
        level=Math.max(getMaxLevel(),level+1);
    }

}
