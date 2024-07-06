package mc.alive.game.effect;

import org.bukkit.entity.Player;

public abstract class Effect {
    public Player p;
    public int ticks;

    abstract public boolean tick();
}
