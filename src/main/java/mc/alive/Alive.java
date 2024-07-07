package mc.alive;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import mc.alive.game.Game;
import mc.alive.game.PlayerData;
import mc.alive.game.TickRunner;
import mc.alive.menu.MainMenu;
import mc.alive.menu.SlotMenu;
import mc.alive.role.hunter.Hunter;
import mc.alive.util.Message;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;

import static mc.alive.game.Game.t_hunter;
import static mc.alive.game.Game.t_survivor;
import static mc.alive.game.PlayerData.getPlayerData;
import static mc.alive.game.TickRunner.chosen_item_display;
import static mc.alive.menu.MainMenu.doc;
import static mc.alive.menu.MainMenu.prepared;

public final class Alive extends JavaPlugin implements Listener {
    public static Alive plugin;
    public static Game game = null;
    public static Scoreboard ms;

    @Override
    public void onEnable() {
        plugin = this;
        ms = Bukkit.getScoreboardManager().getMainScoreboard();
        t_hunter = ms.getTeam("hunter");
        if (t_hunter == null) {
            t_hunter = ms.registerNewTeam("hunter");
            t_hunter.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            t_hunter.color(NamedTextColor.DARK_PURPLE);
        }
        t_survivor = ms.getTeam("survivor");
        if (t_survivor == null) {
            t_survivor = ms.registerNewTeam("survivor");
            t_survivor.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS);
            t_survivor.color(NamedTextColor.DARK_GRAY);
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        registerCommands();
        new TickRunner().runTaskTimer(this, 0, 1);
        Bukkit.getOnlinePlayers().forEach(Game::resetPlayer);
    }

    @Override
    public void onDisable() {
        if (game != null) {
            game.destroy();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Game.resetPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (game == null || game.chooseRole != null) return;
        if (event.getEntity() instanceof Player player && event.getDamager() instanceof Player damager) {
            var pd_hurt = getPlayerData(player);
            var pd_damager = getPlayerData(damager);
            if (pd_damager.attack_cd > 0) {
//                damager.showTitle(Message.title("", "     %s<red>s".formatted(pd_damager.attack_cd), 0, 1000, 0));
                event.setCancelled(true);
                return;
            }
            pd_damager.attack_cd = pd_damager.getRole().getAttackCD();
            int damage = (pd_damager.getRole().getStrength());
            double damage_dealt = event.isCritical() ? damage * 1.3 : damage;
            pd_hurt.damageOrHeal(damage_dealt);
            event.setDamage(0);
        }
    }

    @EventHandler
    public void avoidDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION || game == null) event.setCancelled(true);
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (game != null) {
            ItemStack item = event.getOffHandItem();
            if (!item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) return;
            var data = item.getItemMeta().getCustomModelData();
            if (data < 10000 || data >= 20000) return;
            PlayerData.getPlayerData(event.getPlayer()).changeSkillValue();
            event.setCancelled(true);
        }
        ItemStack item = event.getOffHandItem();
        if (item.hasItemMeta() || item.getItemMeta().getCustomModelData() == 20000) event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        ItemStack item = event.getItem();
        Player player = event.getPlayer();
        if (game != null) {
            if (game.chooseRole != null && game.chooseRole.handleEvent(player)) return;
            if (game.chooseRole != null) return;
            var pd = game.playerData.get(player);
            //管道
            if (pd.getRole() instanceof Hunter) {
                pd.tryIntoDuct();
            }
            //维修
            var target = chosen_item_display.get(player);
            if (target != null && pd.fix_tick == -1) {
                pd.fix_tick = 20;
                return;
            }
        }
        if (
                item == null
                        || !item.hasItemMeta()
                        || !item.getItemMeta().hasCustomModelData()
                        || event.getAction() == Action.LEFT_CLICK_AIR
                        || event.getAction() == Action.LEFT_CLICK_BLOCK
        ) return;
        //使用物品
        if (game != null) {
            var data = item.getItemMeta().getCustomModelData();
            if (data >= 10000 && data < 20000) {
                PlayerData.getPlayerData(player).useSkill();
                event.setCancelled(true);
            }
        } else {
            switch (item.getItemMeta().getCustomModelData()) {
                case 20000 -> {
                    player.openInventory(new MainMenu(player).getInventory());
                    player.playSound(player, Sound.BLOCK_BARREL_OPEN, 1, 1);
                }
                case 20001 -> {
                }
            }
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        var item = event.getCurrentItem();
        if (event.getRawSlot() > event.getInventory().getSize()) {
            if (item != null) {
                if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
                    var data = item.getItemMeta().getCustomModelData();
                    if (data >= 10000 && data <= 20000) event.setCancelled(true);
                }
            }
        }
        if (!(event.getInventory().getHolder() instanceof SlotMenu menu)) return;
        menu.handleClick(event.getSlot());
        event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().hasItemMeta()) {
            int i = event.getItemDrop().getItemStack().getItemMeta().getCustomModelData();
            if (i >= 10000 && i <= 20000) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        var player = event.getPlayer();
        if (doc.contains(player)) {
            player.setGameMode(GameMode.ADVENTURE);
            doc.remove(player);
        }
        if (game == null || game.chooseRole != null) return;
        var pd = getPlayerData(player);
        pd.fix_tick = -1;
        if (pd.getRole() instanceof Hunter) {
            var from = event.getFrom();
            var to = event.getTo();
            if (to.getBlock().getType() == Material.GRAY_STAINED_GLASS_PANE) {
                player.teleport(to.add(to.clone().subtract(from)));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();
        doc.remove(player);
        prepared.remove(player);
        if (game != null) {
            game.end();
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private void registerCommands() {
        var manager = getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            var cs = event.registrar();
            cs.register(
                    Commands.literal("reset")
                            .executes(ctx -> {
                                if (ctx.getSource().getSender() instanceof Player) {
                                    game = null;
                                    Bukkit.getOnlinePlayers().forEach(Game::resetPlayer);
                                }
                                return Command.SINGLE_SUCCESS;
                            }).build(),
                    "重置游戏",
                    List.of("ar")
            );
        });
    }


    @EventHandler
    public void at(AsyncChatEvent event) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            String string = Message.msg.serialize(event.message());
            if (string.equals("@%s".formatted(player.getName()))) {
                string = "%s".formatted(player.getName());
                Player atplayer = Bukkit.getPlayer(string);
                if (atplayer != null) {
                    atplayer.showTitle(Message.title("", "<green>--<red><bold>你被@了</bold><green>--", 100, 1000, 100));
                    atplayer.playSound(atplayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 2f, 2f);
                    atplayer.playSound(atplayer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2f, 2f);
                }
                event.setCancelled(true);
                Bukkit.broadcast(Message.rMsg("<%s> <aqua>%s".formatted(event.getPlayer().getName(), string)));
            }
        });
    }


    @EventHandler
    public void onChat(AsyncChatEvent event) {
//        if (game == null) return;
//        event.renderer(((source, sourceDisplayName, message, viewer) -> {
//            if (game.chooseRole != null || !(viewer instanceof Player player)) return Component.empty();
//            else {
//                var ps = PlayerData.getPlayerData(source);
//                var pv = PlayerData.getPlayerData(player);
//                if (ps == null || pv == null) return Component.empty();
//                if (ps.getRole() instanceof Hunter) {
//                    return Component.text("-")
//                            .append(sourceDisplayName)
//                            .append(Component.text(" : "))
//                            .append(message.decoration(TextDecoration.OBFUSCATED, true));
//                } else {
//                    //船员发出
//                    if (pv.getRole() instanceof Hunter) {
//                        message = message.decoration(TextDecoration.OBFUSCATED, true);
//                    } else {
//                        //todo
//                        player.stopSound(Sound.ENTITY_VILLAGER_AMBIENT);
//                        player.playSound(player, Sound.ENTITY_VILLAGER_AMBIENT, 1, 1);
//                    }
//                    return Component.text("-")
//                            .append(sourceDisplayName)
//                            .append(Component.text(" : ")
//                                    .append(message));
//                }
//            }
//        }));
    }
}
