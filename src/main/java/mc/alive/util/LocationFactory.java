package mc.alive.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Door;
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

    public static void setOpen2x2Door(Location start, BlockFace face, boolean open) {
        int[][] offsets = switch (face) {
            case NORTH, SOUTH -> new int[][]{{0, 0, 0}, {0, 0, 1}};
            case EAST, WEST -> new int[][]{{1, 0, 0}, {0, 0, 0}};
            default -> throw new IllegalArgumentException("Unsupported BlockFace: " + face);
        };

        for (int[] offset : offsets) {
            Block block = start.clone().add(offset[0], offset[1], offset[2]).getBlock();
            var data = (Door) block.getBlockData();
            data.setOpen(open);
            block.setBlockData(data);
        }
    }

    public static void replace2x2Lift(Location loc, Material material, BlockFace face) {
        int[][] offsets = switch (face) {
            case SELF -> new int[][]{{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1}};
            case NORTH -> new int[][]{{1, 0, 1}, {1, -1, 1}, {2, -1, 1}, {2, 0, 1}};
            case EAST -> new int[][]{{-1, 0, 1}, {-1, -1, 1}, {-1, -1, 2}, {-1, 0, 2}};
            case WEST -> new int[][]{{1, 0, -1}, {1, -1, -1}, {1, -1, -2}, {1, 0, -2}};
            case SOUTH -> new int[][]{{-1, 0, -1}, {-1, -1, -1}, {-2, -1, -1}, {-2, 0, -1}};
            default -> throw new IllegalArgumentException("Unsupported BlockFace: " + face);
        };
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
}
