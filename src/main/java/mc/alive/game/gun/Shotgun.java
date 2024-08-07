package mc.alive.game.gun;

import io.papermc.paper.entity.LookAnchor;
import mc.alive.game.PlayerData;
import mc.alive.util.Factory;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

import static mc.alive.Alive.game;

public abstract class Shotgun extends Gun {
    protected Shotgun(ItemStack item, float reactiveForce, BulletType bulletType, double damage, int capacity, long shoot_interval, int reload_time) {
        super(item, reactiveForce, bulletType, damage, capacity, shoot_interval, reload_time);
    }

    abstract double getSpread();

    abstract int getBulletsCount();

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

    @Override
    public void shoot(Player player) {
        if (count == 0) {
            player.sendActionBar(Component.text("null"));
            return;
        }
        if (!canShoot) return;

        //间隔
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                canShoot = true;
                timer.cancel();
            }
        }, shoot_interval);
        canShoot = false;

        count--;

        player.sendActionBar(Component.text("%s / %s".formatted(count, capacity)));
        player.playSound(player, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1f, 1f);
        for (List<Location> locations : shootPath_shotgun(player)) {
            for (Location location : locations) {
                player.getWorld().spawnParticle(Particle.DUST, location, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.ORANGE, 1f), true);
                if (game.hunter.getBoundingBox().contains(location.toVector())) {
                    PlayerData.getPlayerData(game.hunter).damageOrHeal(damage);
                    break;
                }
                if (location.getBlock().isSolid()) break;
            }
        }

        //后坐力
        var l = player.getEyeLocation().add(player.getEyeLocation().getDirection().add(new Vector(0, reactiveForce, 0)));
        player.lookAt(l.getX(), l.getY(), l.getZ(), LookAnchor.EYES);
    }
}
