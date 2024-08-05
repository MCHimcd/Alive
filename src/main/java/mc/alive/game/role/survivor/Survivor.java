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

    //射击路径
    public Result shootPath() {
        var result = player.getWorld().rayTrace(
                player.getEyeLocation(),
                player.getLocation().getDirection(),
                100,
                FluidCollisionMode.NEVER,
                true,
                0.1,
                entity -> entity instanceof Player p && p.equals(game.hunter)
        );
        if (result != null) {
            var target = result.getHitEntity();
            if (target != null) {
                var position = result.getHitPosition();
                return new Result(Factory.line(player.getEyeLocation().subtract(0, 1, 0), position.toLocation(player.getWorld()).subtract(0, 1, 0), 0.5), (Player) target);
            }
        }
        var end = player.getEyeLocation().add(player.getLocation().getDirection().normalize().multiply(100));
        return new Result(Factory.line(player.getEyeLocation().subtract(0, 1, 0), end, 0.5), null);
    }

    public record Result(List<Location> path, Player target) {
        public Result(List<Location> path) {
            this(path, null);
        }
    }
}
