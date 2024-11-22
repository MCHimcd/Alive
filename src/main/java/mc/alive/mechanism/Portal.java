package mc.alive.mechanism;

import mc.alive.tick.TickRunnable;
import org.bukkit.Location;

import static mc.alive.Game.game;

public class Portal implements TickRunnable {
    private final Location location;

    private Portal(Location location) {
        this.location = location;
        startTick();
    }

    public static void summon() {
        Location l1 = null, l2 = null;
        //todo随机位置
        new Portal(l1);
        new Portal(l2);
    }

    @Override
    public void tick() {
        location.getNearbyPlayers(1).forEach(player -> {
            if (game.hunter.equals(player)) return;
            game.survivorRunOut(player);
        });
    }
}
