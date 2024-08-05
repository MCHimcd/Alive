package mc.alive.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public final class Factory {
    // 更改玩家视角
    static public void setYawPitch(float yaw, float pitch, Player player) {
        Location loc = player.getLocation();
        loc.setYaw(yaw);
        loc.setPitch(pitch);
        player.teleport(loc);
    }

    // 直线
    static public List<Location> line(Location start, Location end, double step) {
        List<Location> locations = new ArrayList<>();
        start = start.clone().add(0, 1, 0);
        end = end.clone().add(0, 1, 0);
        Vector direction = end.clone().subtract(start).toVector().normalize().multiply(step);
        while (start.clone().add(direction).distance(end) > step) {
            locations.add(start.clone());
            start.add(direction);
        }
        start.add(direction);
        locations.add(start.clone());
        return locations;
    }

    // 根据玩家朝向矫正点方向
    public static Location roloc(Player player, double x, double y, double z, double rd1) {
        float Yaw = player.getEyeLocation().getYaw();
        double rd = Math.toRadians(Yaw) + rd1;
        Location o = player.getLocation();
        o.setPitch(0);
        double x1 = player.getLocation().getX() + x;
        double y1 = player.getLocation().getY() + y;
        double z1 = player.getLocation().getZ() + z;
        double dx = x1 -= o.getX();
        double dz = z1 -= o.getZ();
        double newX = dx * Math.cos(rd) - dz * Math.sin(rd) + o.getX();
        double newZ = dz * Math.cos(rd) + dx * Math.sin(rd) + o.getZ();
        return new Location(player.getWorld(), newX, y1, newZ);
    }

    // 攻击范围
    public static List<Location> attackRange(double range, Player player) {
        List<Location> locations = new ArrayList<>();
        for (double x = -range, y = 0; x < range; x += 0.1, y += 0.05) {
            //曲线方程
            double a = 0.25 * x * x;
            Location loc = roloc(player, a, y, -x, 30);
            loc.add(player.getEyeLocation().getDirection().normalize().multiply(range));
            locations.add(loc);
        }
        for (double x = range, y = 0; x > -range; x -= 0.1, y += 0.05) {
            //曲线方程
            double a = 0.25 * x * x;
            Location loc = roloc(player, a, y, -x, 30);
            loc.add(player.getEyeLocation().getDirection().normalize().multiply(range));
            locations.add(loc);
        }
        return locations;
    }
}
