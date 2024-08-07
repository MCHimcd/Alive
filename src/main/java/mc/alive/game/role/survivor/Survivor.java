package mc.alive.game.role.survivor;

import mc.alive.game.role.Role;
import mc.alive.util.Factory;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

import static mc.alive.Alive.game;


abstract public class Survivor extends Role {
    public Survivor(Player pl) {
        super(pl);
    }

    //护盾
    abstract public int getMaxShield();

}
