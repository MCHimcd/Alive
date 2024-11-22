package mc.alive.tick;

import mc.alive.Game;
import mc.alive.effect.Giddy;
import mc.alive.item.PickUp;
import mc.alive.role.hunter.Hunter;
import mc.alive.role.survivor.Survivor;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

import static mc.alive.Alive.roles_config;
import static mc.alive.Game.game;
import static mc.alive.PlayerData.of;
import static mc.alive.util.Message.rMsg;

public class PlayerTickrunnable implements TickRunnable {

    public static final Map<Player, ItemDisplay> chosen_item_display = new HashMap<>();
    public static final Map<Player, Item> chosen_item = new HashMap<>();
    public static Location chosen_duct = null;

    @Override
    public void tick() {
        if (game.isPaused) return;
        //选择item_display
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
                        var name = (String) roles_config.get(String.valueOf(game.chooseRole.roles.get(td)));
                        if (name != null) {
                            player.sendActionBar(rMsg("你当前选择的角色为: %s ".formatted(name.split(" ")[1])));
                            td.setGlowing(true);
                        }
                    } else {
                        double progress = (double) game.signal_repeaters.get(td).getProgress() / 400;
                        int a = (int) (progress * 40);
                        player.sendActionBar(rMsg("<yellow>" + "|".repeat(a) + "<white>" + "|".repeat(40 - a) + "     <red> %d / 400".formatted(game.signal_repeaters.get(td).getProgress())));
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
                }
            }
        });

        //选择item
        chosen_item.clear();
        if (Game.isRunning()) {
            game.playerData.keySet().forEach(player -> {
                var r = player.getWorld().rayTrace(
                        player.getEyeLocation(),
                        player.getLocation().getDirection(),
                        2,
                        FluidCollisionMode.NEVER,
                        true,
                        0.5,
                        entity -> {
                            if (entity.getType() != EntityType.ITEM) return false;
                            var item = (Item) entity;
                            PickUp pickUp = game.item_on_ground.get(item);
                            var pd = of(player);
                            return pickUp != null && switch (pickUp) {
                                case SURVIVOR -> pd.getRole() instanceof Survivor;
                                case HUNTER -> pd.getRole() instanceof Hunter;
                                default -> true;
                            };
                        }
                );
                if (r != null) {
                    var item = (Item) r.getHitEntity();
                    if (item != null) {
                        chosen_item.put(player, item);
                    }
                }
            });

        }

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

        //倒地
        game.survivors.forEach(pl -> {
            var pd = of(pl);
            var s = ((Survivor) pd.getRole());
            if (s.isDown()) {
                pd.addEffect(new Giddy(pl, 1));
            }
        });
    }
}
