package mc.alive;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import mc.alive.game.Game;
import mc.alive.game.PlayerData;
import mc.alive.game.TickRunner;
import mc.alive.game.effect.Giddy;
import mc.alive.game.gun.CabinGuardian;
import mc.alive.game.gun.ChamberPistol;
import mc.alive.game.gun.ChamberShotgun;
import mc.alive.game.role.hunter.Hunter;
import mc.alive.game.role.survivor.Survivor;
import mc.alive.menu.MainMenu;
import mc.alive.menu.SlotMenu;
import mc.alive.util.ItemBuilder;
import mc.alive.util.Message;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;

import static mc.alive.game.Game.t_hunter;
import static mc.alive.game.Game.t_survivor;
import static mc.alive.game.PlayerData.getPlayerData;
import static mc.alive.game.TickRunner.chosen_item_display;
import static mc.alive.menu.MainMenu.doc;
import static mc.alive.menu.MainMenu.prepared;
import static org.bukkit.Bukkit.*;

public final class Alive extends JavaPlugin implements Listener {
    public static Alive plugin;
    public static Game game = null;
    public static Scoreboard ms;

    @Override
    public void onDisable() {
        if (game == null) return;
        game.destroy();
    }

    @Override
    public void onEnable() {
        plugin = this;
        ms = getScoreboardManager().getMainScoreboard();

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

        getPluginManager().registerEvents(this, this);

        registerCommands();

        new TickRunner().runTaskTimer(this, 0, 1);

        getOnlinePlayers().forEach(Game::resetPlayer);
    }

    @SuppressWarnings("UnstableApiUsage")
    private void registerCommands() {
        var manager = getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            var cs = event.registrar();
            cs.register(
                    Commands.literal("reset")
                            .executes(ctx -> {
                                if (ctx.getSource().getSender() instanceof Player && game != null) {
                                    game.end();
                                    game = null;
                                    getOnlinePlayers().forEach(Game::resetPlayer);
                                }
                                return Command.SINGLE_SUCCESS;
                            }).build(),
                    "重置游戏",
                    List.of("ar")
            );
            cs.register(
                    Commands.literal("gun")
                            .executes(ctx -> {
                                if (ctx.getSource().getSender() instanceof Player pl && game != null) {
                                    var it = ItemBuilder.getGunItemStack(80000);
                                    game.guns.put(it, new ChamberPistol(it));
                                    pl.getInventory().addItem(it);
                                    var it2 = ItemBuilder.getGunItemStack(80001);
                                    game.guns.put(it2, new ChamberShotgun(it2));
                                    pl.getInventory().addItem(it2);
                                    var it3 = ItemBuilder.getGunItemStack(80002);
                                    game.guns.put(it3, new CabinGuardian(it3));
                                    pl.getInventory().addItem(it3);
                                    for (int i = 0; i < 5; i++) {
                                        pl.getInventory().addItem(
                                                ItemBuilder.material(Material.DIAMOND, 90001)
                                                        .name(Component.text("舱室标准弹"))
                                                        .amount(64)
                                                        .build()
                                        );
                                    }

                                }
                                return Command.SINGLE_SUCCESS;
                            }).build(),
                    "枪",
                    List.of()
            );
        });
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
                event.setCancelled(true);
                return;
            }
            pd_damager.attack_cd = pd_damager.getRole().getAttackCD();
            if (pd_damager.getRole() instanceof Hunter) {
                pd_hurt.damageOrHeal(pd_damager.getRole().getStrength());
            } else {
                event.setCancelled(true);
            }
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
            var pd = game.playerData.get(event.getPlayer());
            ItemStack item = event.getOffHandItem();

            if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
                var data = item.getItemMeta().getCustomModelData();

                if (data >= 10000 && data < 20000) {
                    //技能
                    PlayerData.getPlayerData(event.getPlayer()).changeSkillValue();
                    event.setCancelled(true);
                } else if (data >= 80000 && data < 90000 && getPlayerData(event.getPlayer()).getRole() instanceof Survivor) {
                    //枪
                    game.guns.get(item).reload(event.getPlayer());
                    event.setCancelled(true);
                }
            }

            if (pd.hasEffect(Giddy.class)) {
                event.setCancelled(true);
            }
        }

        ItemStack item = event.getOffHandItem();
        if (item.hasItemMeta() && item.getItemMeta().getCustomModelData() == 20000) {
            event.setCancelled(true);
        }
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
            if (pd.hasEffect(Giddy.class)) {
                event.setCancelled(true);
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
                //技能
                PlayerData.getPlayerData(player).useSkill();
                event.setCancelled(true);
            } else if (data >= 80000 && data < 90000 && getPlayerData(player).getRole() instanceof Survivor) {
                //枪
                game.guns.get(item).startShoot(player);
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

    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        var item = event.getCurrentItem();
        if (event.getRawSlot() > event.getInventory().getSize() && item != null && item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            var data = item.getItemMeta().getCustomModelData();
            if ((data >= 10000 && data <= 20000) || data == 90000) {
                event.setCancelled(true);
            }
        }

        if (!(event.getInventory().getHolder() instanceof SlotMenu menu)) return;

        menu.handleClick(event.getSlot());
        event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        var itemStack = event.getItemDrop().getItemStack();
        if (itemStack.hasItemMeta()) {
            var itemMeta = itemStack.getItemMeta();
            if (itemMeta.hasCustomModelData()
                    && (itemMeta.getCustomModelData() >= 10000 && itemMeta.getCustomModelData() <= 20000)
                    || itemMeta.getCustomModelData() == 90000
            ) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onStopUsing(PlayerStopUsingItemEvent event) {
        if (game == null || game.chooseRole != null) return;
        var player = event.getPlayer();
        var gun = game.guns.get(player.getInventory().getItemInMainHand());
        if (gun != null) {
            gun.stopShoot(player);
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (game == null || game.chooseRole != null) return;
        if (event.getEntityType() == EntityType.ARROW) event.setCancelled(true);
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
        if (pd.hasEffect(Giddy.class)) {
            event.setCancelled(true);
            return;
        } else if (!pd.canMove()) {
            player.teleport(event.getFrom().setDirection(event.getTo().getDirection()));
            event.setCancelled(true);
            return;
        }

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

    @EventHandler
    public void at(AsyncChatEvent event) {
        String messageString = Message.msg.serialize(event.message());
        getOnlinePlayers().forEach(player -> {
            String atString = "@%s".formatted(player.getName());
            if (messageString.equals(atString)) {
                Player atplayer = getPlayer(player.getName());
                if (atplayer != null) {
                    atplayer.showTitle(Message.title("", "<green>--<red><bold>你被@了</bold><green>--", 0, 1000, 0));
                    new BukkitRunnable() {
                        int time = 20;

                        @Override
                        public void run() {
                            atplayer.playSound(atplayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 10f, 10f);
                            atplayer.playSound(atplayer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 10f, 10f);
                            if (time-- <= 0) {
                                cancel();
                            }
                        }
                    }.runTaskTimer(plugin, 0, 1);
                }
                event.setCancelled(true);
                broadcast(Message.rMsg("<%s> <aqua>%s".formatted(event.getPlayer().getName(), player.getName())));
            }
        });
    }

    @EventHandler
    public void onChangeMainHandItem(PlayerItemHeldEvent event) {
        if (game == null || game.chooseRole != null) return;
        var player = event.getPlayer();
        //noinspection DataFlowIssue
        player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(255);
        var pre_it = player.getInventory().getItem(event.getPreviousSlot());
        if (pre_it == null || !pre_it.hasItemMeta() || !pre_it.getItemMeta().hasCustomModelData()) return;
        var data1 = pre_it.getItemMeta().getCustomModelData();
        if (data1 >= 80000 && data1 < 90000) {
            game.guns.get(pre_it).handleItemChange(player, true);
        }
        var new_it = player.getInventory().getItem(event.getNewSlot());
        if (new_it == null || !new_it.hasItemMeta() || !new_it.getItemMeta().hasCustomModelData()) return;
        var data2 = new_it.getItemMeta().getCustomModelData();
        if (data2 >= 80000 && data2 < 90000) {
            game.guns.get(new_it).handleItemChange(player, false);
        }
    }

    //    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (game == null) return;

        event.renderer((source, sourceDisplayName, message, viewer) -> {
            if (!(viewer instanceof Player player)) return Component.empty();

            var ps = PlayerData.getPlayerData(source);
            var pv = PlayerData.getPlayerData(player);
            if (ps == null || pv == null) return Component.empty();

            if (ps.getRole() instanceof Hunter) {
                return Component.text("-")
                        .append(sourceDisplayName)
                        .append(Component.text(" : "))
                        .append(message.decoration(TextDecoration.OBFUSCATED, true));
            } else {
                if (pv.getRole() instanceof Hunter) {
                    message = message.decoration(TextDecoration.OBFUSCATED, true);
                } else {
                    player.stopSound(Sound.ENTITY_VILLAGER_AMBIENT);
                    player.playSound(player, Sound.ENTITY_VILLAGER_AMBIENT, 1, 1);
                }
                return Component.text("-")
                        .append(sourceDisplayName)
                        .append(Component.text(" : ").append(message));
            }
        });
    }
}
