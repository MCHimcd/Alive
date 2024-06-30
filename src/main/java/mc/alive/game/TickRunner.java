package mc.alive.game;

import mc.alive.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

import static mc.alive.Alive.game;

public class TickRunner extends BukkitRunnable {
    public static final Map<Player, ItemDisplay> chosen_item_display = new HashMap<>();
    public static Location chosen_duct = null;

    @Override
    public void run() {
        if (game == null) return;
        game.playerData.values().forEach(PlayerData::tick);
        //i_d
        chosen_item_display.values().forEach(e -> e.setGlowing(false));
        chosen_item_display.clear();
        Bukkit.getOnlinePlayers().forEach(player -> {
            var r = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getLocation().getDirection(), 4, 0.5, entity -> entity.getType() == EntityType.ITEM_DISPLAY);
            if (r != null) {
                var td = (ItemDisplay) r.getHitEntity();
                if (td != null) {
                    if (game.chooseRole != null) td.setGlowing(true);
                    else if (!player.equals(game.hunter)) {
                        double progress = (double) game.fix(td, 0) / 400;
                        int a = (int) (progress * 40);
                        player.sendActionBar(Message.rMsg("<yellow>" + "|".repeat(a) + "<white>" + "|".repeat(40 - a)+ "     <red> %d / 400".formatted(game.fix(td,0))));
                    }
                    chosen_item_display.put(player, td);
                }
            }
        });
        //duct
        chosen_duct = null;
        var player = game.hunter;
        var r = player.getWorld().rayTrace(player.getEyeLocation(), player.getLocation().getDirection(), 3, FluidCollisionMode.NEVER, true, 0.5, entity -> entity.getType() == EntityType.MARKER);
        if (r != null) {
            var m = r.getHitEntity();
            if (m != null) {
                chosen_duct = m.getLocation();
            }
        }
    }
}
