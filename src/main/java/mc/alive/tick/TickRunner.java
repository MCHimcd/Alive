package mc.alive.tick;

import mc.alive.effect.Effect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static mc.alive.Game.game;

public final class TickRunner extends BukkitRunnable {
    public static final List<TickRunnable> tickRunnable = new LinkedList<>();
    public static final List<Effect> effectList = new CopyOnWriteArrayList<>();
    public static boolean gameEnd = false;

    public TickRunner() {
        tickRunnable.addAll(List.of(
                new PlayerTickrunnable(),
                new MechanismTickrunnable(),
                new TriggerableTickrunnable()
        ));
    }

    @Override
    public void run() {
        if (game == null) return;
        if (gameEnd) {
            game.end(null);
            gameEnd = false;
            return;
        }
        tickRunnable.forEach(TickRunnable::tick);
        effectList.forEach(Effect::tick);
    }
}
