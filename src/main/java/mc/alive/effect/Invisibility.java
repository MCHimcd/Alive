package mc.alive.effect;

import org.bukkit.entity.Player;

import static mc.alive.Alive.plugin;
import static mc.alive.Game.game;

public class Invisibility extends Effect {

    public Invisibility(Player player, int tick) {
        super(player, tick);
    }

    @Override
    protected boolean run() {
        for (Player survivor : game.survivors) {
            if (remained_tick <= 0)
                survivor.showEntity(plugin, player);
            else survivor.hidePlayer(plugin, player);
        }
        return true;
    }
}
