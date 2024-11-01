package mc.alive.effect;

import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
            ar.setInvisible(true);
            ar.setGlowing(true);
            ar.getEquipment().setHelmet(new ItemStack(Material.PLAYER_HEAD));
            game.markers.add(ar);
            if (game.survivors.contains(player)) {
                game.survivors.forEach(pl -> pl.hideEntity(plugin, ar));
            } else game.hunter.hideEntity(plugin, ar);
            new BukkitRunnable() {
                @Override
                public void run() {
                    game.markers.remove(ar);
                    ar.remove();
                }
            }.runTaskLater(plugin, 1);
        });
        return true;
    }
}
