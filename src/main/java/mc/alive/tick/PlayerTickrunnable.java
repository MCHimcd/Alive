package mc.alive.tick;

import mc.alive.game.role.Role;
import mc.alive.game.role.hunter.Hunter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

import static mc.alive.game.Game.game;
import static mc.alive.game.PlayerData.of;
import static mc.alive.util.Message.rMsg;

public class PlayerTickrunnable implements TickRunnable {

    public static final Map<Player, ItemDisplay> chosen_item_display = new HashMap<>();
    public static Location chosen_duct = null;

    @Override
    public void tick() {
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
                    if (game.chooseRole != null) {
                        player.sendActionBar(Component.text("你当前选择的角色为: %s ".formatted(Role.names.get(game.chooseRole.roles.get(td)))));
                        td.setGlowing(true);
                    } else if (!player.equals(game.hunter)) {
                        double progress = (double) game.fix(td, 0) / 400;
                        int a = (int) (progress * 40);
                        player.sendActionBar(rMsg("<yellow>" + "|".repeat(a) + "<white>" + "|".repeat(40 - a) + "     <red> %d / 400".formatted(game.fix(td, 0))));
                    }
                    chosen_item_display.put(player, td);
                }
            }

            //playerData
            if (game.chooseRole != null) return;
            var pd = of(player);
            if (pd != null) {
                if (pd.getRole() instanceof Hunter) {
                    //hunter
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 1, 0, false, false));
                    player.setFoodLevel(20);
                } else {
                    //幸存者
                    var itemMeta = player.getInventory().getItemInMainHand().getItemMeta();
                    if (itemMeta != null && itemMeta.hasCustomModelData() && itemMeta.getCustomModelData() == 10200) {
                        if (player.getPitch() >= 0) {
                            player.sendActionBar(rMsg(""));
                        } else {
                            player.sendActionBar(rMsg(""));
                        }
                    }
                }
            }
        });

        //管道
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
