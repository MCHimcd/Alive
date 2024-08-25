package mc.alive.effect;

import org.bukkit.entity.Player;

public class Giddy extends Effect {
    public Giddy(Player player, int tick) {
        super(player, tick);
    }

    @Override
    protected boolean run() {
        return true;
    }
}
