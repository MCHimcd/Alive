package mc.alive.tick;

import mc.alive.Game;
import mc.alive.mechanism.Barrier;
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
    public static final Map<Player, Barrier> chosenBarriers = new HashMap<>();

    @Override
    public void tick() {
        if (!Game.isStarted()) return;
        //电梯
        game.lifts.forEach((bd, lift) -> {
            lift.players.clear();
            bd.getWorld().getNearbyPlayers(bd.getLocation(), 3).forEach(player -> {
                if (player.getBoundingBox().overlaps(new BoundingBox(
                        bd.getX() + 0.6, bd.getY() + 0.3, bd.getZ() + 0.6,
                        bd.getX() + 1.4, bd.getY() + 1.3, bd.getZ() + 1.4
                ))) {
                    lift.players.add(player);
                }
            });
        });

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

        //选择屏障
        chosenBarriers.clear();
        game.barriers.forEach((location, barrier) -> {
            barrier.isChosen = false;
            location.getNearbyPlayers(4).forEach(player -> {
                BoundingBox boundingBox = barrier.getBoundingBox();
                RayTraceResult result = boundingBox.rayTrace(player.getEyeLocation().toVector(), player.getEyeLocation().getDirection(), 3);
                if (result != null) {
                    barrier.isChosen = true;
                    chosenBarriers.put(player, barrier);
                }
            });
            if (!barrier.isChosen && barrier.tick_task != null) {
                barrier.tick = 0;
                barrier.tick_task.cancel();
                barrier.tick_task = null;
            }
        });
    }
}
