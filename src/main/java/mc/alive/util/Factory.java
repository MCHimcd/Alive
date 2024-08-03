package mc.alive.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

import static mc.alive.Alive.plugin;

public final class Factory {
    // 更改玩家视角
    static public void setYawPitch(float yaw, float pitch, Player player) {
        Location loc = player.getLocation();
        loc.setYaw(yaw);
        loc.setPitch(pitch);
        player.teleport(loc);
    }

    // 直线
    static public List<Location> line(Location start, Location end,double step) {
        List<Location> locations = new ArrayList<>();
        start = start.clone().add(0, 1, 0);
        end = end.clone().add(0, 1, 0);
        Vector direction = end.clone().subtract(start).toVector().normalize().multiply(step);
        while (start.clone().add(direction).distance(end) > step) {
            locations.add(start.clone());
            start.add(direction);
        }
        return locations;
    }

    // 根据玩家朝向矫正点方向
    public static Location roloc(Player player, double x, double y, double z, double rd1) {
        float yaw = player.getEyeLocation().getYaw();
        double rd = Math.toRadians(yaw) + rd1;
        Location origin = player.getLocation();
        origin.setPitch(0);

        double dx = x - origin.getX();
        double dz = z - origin.getZ();
        double newX = dx * Math.cos(rd) - dz * Math.sin(rd) + origin.getX();
        double newZ = dz * Math.cos(rd) + dx * Math.sin(rd) + origin.getZ();
        return new Location(player.getWorld(), newX, y + origin.getY(), newZ);
    }

    // 攻击范围
    public static List<Location> attackRange(double range, Player player) {
        List<Location> locations = new ArrayList<>();
        for (double x = -4, y = 0; x <= 0; x += 0.1, y += 0.005) {
            if (addLocation(range, player, locations, x, y)) break;
        }
        for (double x = 4, y = 0; x >= 0; x -= 0.1, y += 0.005) {
            if (addLocation(range, player, locations, x, y)) break;
        }
        return locations;
    }

    private static boolean addLocation(double range, Player player, List<Location> locations, double x, double y) {
        double a = 0.5 * x * x;
        Location loc = roloc(player, a, y, x, 30);
        loc.add(player.getLocation().getDirection().normalize().multiply(range)).add(0, 1, 0);
        if (!(loc.getBlock().isPassable() || loc.getBlock().isLiquid())) return true;
        locations.add(loc.clone());
        return false;
    }
}
