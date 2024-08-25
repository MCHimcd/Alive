package mc.alive.effect;

import org.bukkit.entity.Player;

public abstract class MultilevelEffect extends Effect {
    protected int level;

    public MultilevelEffect(Player player, int tick, int level) {
        super(player, tick);
        this.level = level;
    }

    protected abstract boolean run();
}
