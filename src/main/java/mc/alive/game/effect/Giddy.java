package mc.alive.game.effect;

import org.bukkit.entity.Player;

public class Giddy extends Effect{
    Giddy(Player p, int ticks) {
        this.player = p;
        this.tick = ticks;
    }

    @Override
    public boolean tick() {
        tick--;
        return tick<=0;
    }
}
