package mc.alive.game.effect;

import org.bukkit.entity.Player;

public abstract class Effect {
    public Player player;
    public int tick;

    public static Giddy giddy(Player player, int tick) {
        return new Giddy(player, tick);
    }

    abstract public boolean tick();
}

