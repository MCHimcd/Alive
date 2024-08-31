package mc.alive.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class LocationFactory {
    public static Vector3f getTranslation(BlockFace face) {
        return switch (face) {
            case NORTH, EAST -> new Vector3f(-1, -1, 1);
            case SOUTH, WEST -> new Vector3f(2, -1, 0);
            default -> new Vector3f();
        };
    }

    /**
     * @param start 开始
     * @param end   结束
     * @param step  相邻两个点之间的距离
     * @return 生成的直线
     */
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

    public static void replace2x2(Location loc, Material material, BlockFace face) {
        int[][] offsets = getOffsets(face);
        Location[] corners = new Location[4];

        for (int i = 0; i < offsets.length; i++) {
            Location newLoc = loc.clone().add(offsets[i][0], offsets[i][1], offsets[i][2]);
            newLoc.getBlock().setType(material);
            corners[i] = newLoc;
        }

        // 获取2x2区域的最小和最大坐标
        int minX = Math.min(Math.min(corners[0].getBlockX(), corners[1].getBlockX()), Math.min(corners[2].getBlockX(), corners[3].getBlockX()));
        int maxX = Math.max(Math.max(corners[0].getBlockX(), corners[1].getBlockX()), Math.max(corners[2].getBlockX(), corners[3].getBlockX()));
        int minZ = Math.min(Math.min(corners[0].getBlockZ(), corners[1].getBlockZ()), Math.min(corners[2].getBlockZ(), corners[3].getBlockZ()));
        int maxZ = Math.max(Math.max(corners[0].getBlockZ(), corners[1].getBlockZ()), Math.max(corners[2].getBlockZ(), corners[3].getBlockZ()));

        // 遍历所有玩家，检查是否在2x2区域内
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location playerLoc = player.getLocation();
            if (playerLoc.getBlockX() >= minX && playerLoc.getBlockX() <= maxX &&
                    playerLoc.getBlockZ() >= minZ && playerLoc.getBlockZ() <= maxZ) {
                // 传送到目标方块位置
                player.teleport(loc);
            }
        }
    }

    private static int[][] getOffsets(BlockFace face) {
        return switch (face) {
            case SELF -> new int[][]{{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1}};
            case NORTH -> new int[][]{{1, 0, 1}, {1, -1, 1}, {2, -1, 1}, {2, 0, 1}};
            case EAST -> new int[][]{{-1, 0, 1}, {-1, -1, 1}, {-1, -1, 2}, {-1, 0, 2}};
            case WEST -> new int[][]{{1, 0, -1}, {1, -1, -1}, {1, -1, -2}, {1, 0, -2}};
            case SOUTH -> new int[][]{{-1, 0, -1}, {-1, -1, -1}, {-2, -1, -1}, {-2, 0, -1}};
            default -> throw new IllegalArgumentException("Unsupported BlockFace: " + face);
        };
    }
}
