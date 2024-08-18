package mc.alive.game;

import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import static mc.alive.Alive.plugin;

public class Lift {
    private final BlockDisplay blockDisplay;

    public Lift(BlockDisplay blockDisplay) {
        this.blockDisplay = blockDisplay;
    }

    public void run(Player player, boolean up) {
        new BukkitRunnable() {
            private int t = 0;

            public void run() {
                if (t++ >= 30) cancel();
                else {
                    var dy = up ? 0.1 : -0.1;
                    player.teleport(
                            player.getLocation().add(0, dy, 0),
                            TeleportFlag.Relative.YAW,
                            TeleportFlag.Relative.PITCH
                    );
                    blockDisplay.teleport(blockDisplay.getLocation().add(0, dy, 0));
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
