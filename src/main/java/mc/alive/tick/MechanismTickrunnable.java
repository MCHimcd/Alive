package mc.alive.tick;

import mc.alive.Game;
import mc.alive.mechanism.Door;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;

import static mc.alive.Game.game;

public class MechanismTickrunnable implements TickRunnable {
    public static final Map<Player, Door> chosenDoors = new HashMap<>();

    @Override
    public void tick() {
        if (!Game.isStarted()) return;
        //选择密码门
        chosenDoors.clear();
        game.doors.forEach((location, door) ->
                location.getNearbyPlayers(4).forEach(player -> {
                    RayTraceResult result = new BoundingBox(
                            location.getX(), location.getY(), location.getZ(),
                            location.getX() + (door.getFace() == BlockFace.EAST ? 2 : 1), location.getY() + 2, location.getZ() + (door.getFace() == BlockFace.NORTH ? 2 : 1)
                    ).rayTrace(player.getEyeLocation().toVector(), player.getEyeLocation().getDirection(), 3);
                    if (result != null) {
                        chosenDoors.put(player, door);
                    }
                }));

    }
}
