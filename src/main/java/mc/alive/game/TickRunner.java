package mc.alive.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

import static mc.alive.Alive.game;

public class TickRunner extends BukkitRunnable {
    public static final Map<Player, ItemDisplay> chosen = new HashMap<>();

    @Override
    public void run() {
        if (game != null) {
            game.playerData.values().forEach(PlayerData::tick);
        }
        //选择
        chosen.values().forEach(e -> e.setGlowing(false));
        chosen.clear();
        Bukkit.getOnlinePlayers().forEach(player -> {
                var r = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getLocation().getDirection(), 10, 0.5, entity -> entity.getType() == EntityType.ITEM_DISPLAY);
                if (r != null) {
                    var td = (ItemDisplay) r.getHitEntity();
                    if (td != null) {
                        td.setGlowing(true);
                        chosen.put(player, td);
                    }
                }
        });
    }
}
