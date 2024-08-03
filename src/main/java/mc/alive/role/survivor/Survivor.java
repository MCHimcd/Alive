package mc.alive.role.survivor;

import mc.alive.role.Role;
import mc.alive.util.Factory;
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

    //射击路径
    protected List<Location> shootPath() {
        var result = player.getWorld().rayTrace(
                player.getLocation(),
                player.getLocation().getDirection(),
                20,
                FluidCollisionMode.NEVER,
                true,
                1,
                entity -> entity instanceof Player p && p.equals(game.hunter)
        );
        if (result != null) {
            var position = result.getHitPosition();
            return Factory.line(player.getLocation(), position.toLocation(player.getWorld()),0.5);
        }
        return List.of();
    }
}
