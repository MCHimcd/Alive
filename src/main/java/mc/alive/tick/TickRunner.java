package mc.alive.tick;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedList;
import java.util.List;

import static mc.alive.game.Game.game;

public class TickRunner extends BukkitRunnable {
    public static final List<TickRunnable> tickRunnable = new LinkedList<>();
    public static boolean gameEnd = false;

    public TickRunner() {
        tickRunnable.addAll(List.of(
                new PlayerTickrunnable(),
                new MechanismTickrunnable(),
                new StaminaTickrunnable()
        ));
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
