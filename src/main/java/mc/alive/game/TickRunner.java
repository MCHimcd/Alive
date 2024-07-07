package mc.alive.game;

import mc.alive.game.effect.Effect;
import mc.alive.role.hunter.Hunter;
import mc.alive.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

import static mc.alive.Alive.game;

public class TickRunner extends BukkitRunnable {
    public static final Map<Player, ItemDisplay> chosen_item_display = new HashMap<>();
    public static Location chosen_duct = null;
    public static boolean gameEnd = false;

    @Override
    public void run() {
        if (game == null) return;
        if (gameEnd) {
            game.end();
            gameEnd = false;
            return;
        }
        game.playerData.values().forEach(PlayerData::tick);
        //player foreach
        chosen_item_display.values().forEach(e -> e.setGlowing(false));
        chosen_item_display.clear();
        Bukkit.getOnlinePlayers().forEach(player -> {
            var r = player.getWorld().rayTrace(
                    player.getEyeLocation(),
                    player.getLocation().getDirection(),
                    4,
                    FluidCollisionMode.NEVER,
                    true,
                    0.5,
                    entity -> entity.getType() == EntityType.ITEM_DISPLAY
            );
            if (r != null) {
                var td = (ItemDisplay) r.getHitEntity();
                if (td != null) {
                    if (game.chooseRole != null) td.setGlowing(true);
                    else if (!player.equals(game.hunter)) {
                        double progress = (double) game.fix(td, 0) / 400;
                        int a = (int) (progress * 40);
                        player.sendActionBar(Message.rMsg("<yellow>" + "|".repeat(a) + "<white>" + "|".repeat(40 - a) + "     <red> %d / 400".formatted(game.fix(td, 0))));
                    }
                    chosen_item_display.put(player, td);
                }
            }
            //药水效果
            if (game.chooseRole != null) return;
            var pd = PlayerData.getPlayerData(player);
            if (pd.getRole() instanceof Hunter) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 1, 0, false, false));
                player.setFoodLevel(20);
            }
        });
        //duct
        chosen_duct = null;
        var player = game.hunter;
        @SuppressWarnings("UnstableApiUsage")
        var r = player.getWorld().rayTrace(
                player.getEyeLocation(),
                player.getLocation().getDirection(),
                3,
                FluidCollisionMode.NEVER,
                true,
                0.5,
                entity -> entity.getType() == EntityType.MARKER,
                block -> block.getType() != Material.GRAY_STAINED_GLASS_PANE
        );
        if (r != null) {
            var m = r.getHitEntity();
            if (m != null) {
                chosen_duct = m.getLocation();
            }
        }
    }
}
