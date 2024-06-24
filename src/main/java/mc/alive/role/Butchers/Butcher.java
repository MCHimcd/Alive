package mc.alive.role.Butchers;

import mc.alive.role.Role;
import mc.alive.role.Sailors.New;
import mc.alive.role.Sailors.Sailor;
import org.bukkit.entity.Player;

abstract public class Butcher extends Role {
    public Butcher(Player pl) {
        super(pl);
    }


    //最大等级
    abstract public int getMaxLevel();


    public static Butcher getButcher(int id, Player p) {
        return switch (id) {
            case 1 -> new Hunter(p); //狩猎者
            default -> new Hunter(p);
        };
    }

}
