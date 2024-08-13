package mc.alive.game;

import mc.alive.game.role.Role;
import mc.alive.game.role.hunter.Hunter;
import mc.alive.game.role.survivor.Survivor;
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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static mc.alive.Alive.game;
import static mc.alive.game.PlayerData.getPlayerData;
import static mc.alive.util.Message.rMsg;

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
            var pd = getPlayerData(player);
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

        //物品拾取
        List<ItemDisplay> removes = new ArrayList<>();
        game.items.forEach((itemDisplay, pickUp) -> itemDisplay.getWorld().getNearbyPlayers(itemDisplay.getLocation(), 1,
                pl -> switch (pickUp) {
                    case BOTH -> true;
                    case HUNTER -> getPlayerData(pl).getRole() instanceof Hunter;
                    case SURVIVOR -> getPlayerData(pl).getRole() instanceof Survivor;
                }).stream().findAny().ifPresent(player1 -> {
            player1.getInventory().addItem(itemDisplay.getItemStack());
            removes.add(itemDisplay);
        }));
        for (ItemDisplay remove : removes) {
            game.items.remove(remove);
            remove.remove();
        }
    }
}
