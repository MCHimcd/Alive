package mc.alive.effect;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import static mc.alive.Alive.plugin;
import static mc.alive.Game.game;

public class Exposure extends Effect {
    public Exposure(Player player, int tick) {
        super(player, tick);
    }

    @Override
    protected boolean run() {
        player.getWorld().spawn(player.getLocation(), ArmorStand.class, ar -> {
            ar.setMarker(true);
            ar.setGlowing(true);
            if (player.equals(game.hunter)) {
                game.survivors.forEach(pl -> pl.hideEntity(plugin, ar));
            } else game.hunter.hideEntity(plugin, ar);
            new BukkitRunnable() {
                @Override
                public void run() {
                    ar.remove();
                }
            }.runTaskLater(plugin, 1);
        });
        return true;
    }
}
