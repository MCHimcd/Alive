package mc.alive.tick;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedList;
import java.util.List;

import static mc.alive.game.Game.game;

public class TickRunner extends BukkitRunnable {
    public static final List<TickRunnable> tickRunnable = new LinkedList<>();
    public static boolean gameEnd = false;

    public TickRunner() {
        tickRunnable.add(new PlayerTickrunnable());
        tickRunnable.add(new LiftTickrunnable());
    }

    @Override
    public void run() {
        if (game == null) return;
        if (gameEnd) {
            game.end();
            gameEnd = false;
            return;
        }
        tickRunnable.forEach(TickRunnable::tick);
    }
}
