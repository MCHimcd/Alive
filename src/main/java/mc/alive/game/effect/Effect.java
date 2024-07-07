package mc.alive.game.effect;

import org.bukkit.entity.Player;

public abstract class Effect {
    private final Player player;
    private final int time;

    abstract public boolean tick();

    public Effect(Player player, int ticks) {
        this.player = player;
        this.time = ticks;
    }
}
