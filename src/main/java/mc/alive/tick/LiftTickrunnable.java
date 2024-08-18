package mc.alive.tick;

import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.HashMap;
import java.util.Map;

import static mc.alive.game.Game.instance;

public class LiftTickrunnable implements TickRunnable {
    public static final Map<Player, BlockDisplay> player_in_lift = new HashMap<>();

    @Override
    public void tick() {
        player_in_lift.clear();
        instance.lifts.forEach(bd -> {
            bd.getWorld().getNearbyPlayers(bd.getLocation(), 3).forEach(player -> {
                if (player.getBoundingBox().overlaps(new BoundingBox(
                        bd.getX(), bd.getY() + 0.3, bd.getZ(),
                        bd.getX() + 2, bd.getY() + 1.3, bd.getZ() + 3
                ))) {
                    player_in_lift.put(player, bd);
                }
            });
        });

        player_in_lift.forEach((player, blockDisplay) -> {
            player.teleport(player.getLocation().add(0, 0.1, 0));
            blockDisplay.teleport(blockDisplay.getLocation().add(0, 0.1, 0));
        });
    }
}
