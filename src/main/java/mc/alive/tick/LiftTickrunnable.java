package mc.alive.tick;

import org.bukkit.util.BoundingBox;

import static mc.alive.game.Game.game;

public class LiftTickrunnable implements TickRunnable {

    @Override
    public void tick() {
        game.lifts.forEach((bd, lift) -> bd.getWorld().getNearbyPlayers(bd.getLocation(), 3).forEach(player -> {
            if (game.player_in_lift.containsKey(player)) return;
            if (player.getBoundingBox().overlaps(new BoundingBox(
                    bd.getX() + 0.8, bd.getY() + 0.3, bd.getZ() + 0.8,
                    bd.getX() + 1.2, bd.getY() + 1.3, bd.getZ() + 1.2
            ))) {
                lift.run(player, true);
                game.player_in_lift.put(player, bd);
            }
        }));
    }
}
