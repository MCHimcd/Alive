package mc.alive.game.item.usable.gun;

import mc.alive.game.PlayerData;
import mc.alive.game.item.GameItem;
import mc.alive.util.Factory;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static mc.alive.game.Game.game;

public abstract class Shotgun extends Gun {
    protected Shotgun(float reactiveForce, Class<? extends GameItem> bulletType, double damage, int capacity, long shoot_interval, int reload_time) {
        super(reactiveForce, bulletType, damage, capacity, shoot_interval, reload_time);
    }

    @Override
    public boolean shoot(Player player) {
        if (cannotShoot(player)) return false;

        for (List<Location> locations : shootPath_shotgun(player)) {
            for (Location location : locations) {
                player.getWorld().spawnParticle(Particle.DUST, location, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.ORANGE, 1f), true);
                if (game.hunter.getBoundingBox().contains(location.toVector())) {
                    PlayerData.of(game.hunter).damageOrHeal(damage);
                    break;
                }
                if (location.getBlock().isSolid()) break;
            }
        }

        applyRecoil(player);
        return true;
    }

    protected List<List<Location>> shootPath_shotgun(Player player) {
        Random random = new Random();
        var direction = player.getLocation().getDirection();
        var locations = new ArrayList<List<Location>>();

        for (int i = 0; i < getBulletsCount(); i++) {
            var newDirection = direction.clone();
            var spread = getSpread();
            newDirection.add(new Vector(
                    (random.nextDouble() - 0.5) * spread,
                    (random.nextDouble() - 0.5) * spread,
                    (random.nextDouble() - 0.5) * spread
            ));
            locations.add(Factory.line(
                    player.getEyeLocation().subtract(0, 1, 0),
                    player.getEyeLocation().add(newDirection.normalize().multiply(20)).subtract(0, 1, 0),
                    0.5
            ));
        }
        return locations;
    }

    abstract double getSpread();

    abstract int getBulletsCount();
}
