package mc.alive.role.Sailors;

import mc.alive.role.Role;
import mc.alive.role.Skill;
import org.bukkit.entity.Player;

abstract public class Sailor extends Role {
    public Sailor(Player pl) {
        super(pl);
    }

    //护盾 （仅船员）
    abstract public int getShield();


    public static Sailor getSailor(int id, Player p) {
        return switch (id) {
            case 1 -> new New(p);
            default -> new New(p);
        };
    }
}
