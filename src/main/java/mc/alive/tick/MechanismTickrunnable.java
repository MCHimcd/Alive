package mc.alive.tick;

import org.bukkit.util.BoundingBox;

import static mc.alive.Game.game;

public class MechanismTickrunnable implements TickRunnable {

    @Override
    public void tick() {
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
    }
}
