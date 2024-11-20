package mc.alive.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import mc.alive.Game;
import mc.alive.PlayerData;
import mc.alive.StoredData;
import mc.alive.effect.Giddy;
import mc.alive.effect.Invisibility;
import mc.alive.effect.Slowness;
import mc.alive.menu.SlotMenu;
import mc.alive.role.hunter.Hunter;
import mc.alive.role.survivor.Survivor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static mc.alive.Game.game;
import static mc.alive.StoredData.playerStoredData;
import static mc.alive.menu.MainMenu.players_looking_document;
import static mc.alive.menu.MainMenu.prepared_players;
import static mc.alive.util.Message.rMsg;

public class PlayerListener implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Optional<StoredData> data = StoredData.data.stream().filter(d -> d.getName().equals(player.getName())).findFirst();
        if (data.isPresent())
            playerStoredData.put(player, data.get().updateDate());
        else {
            StoredData new_data = new StoredData(player.getName());
            StoredData.data.add(new_data);
            playerStoredData.put(player, new_data);
        }

        AtomicReference<PlayerData> pd = new AtomicReference<>();
        if (Game.isStarted()) {
            AtomicReference<Player> found = new AtomicReference<>();
            game.playerData.forEach((p, d) -> {
                if (p.getName().equals(player.getName())) {
                    d.setPlayer(player);
                    pd.set(d);
                    found.set(p);
                }
            });
            if (found.get() != null) {
                game.playerData.remove(found.get());
                if (player.getName().equals(game.hunter.getName())) game.hunter = player;
                else {
                    game.survivors.remove(found.get());
                    game.survivors.add(player);
                }
            }
        }
        if (pd.get() != null) {
            game.playerData.put(player, pd.get());
            game.isPaused = false;
            if (game.pause_task != null && !game.pause_task.isCancelled()) game.pause_task.cancel();
            player.getWorld().sendMessage(rMsg("<dark_green>游戏已恢复"));
        } else {
            Game.resetPlayer(player);
        }
        setAtChatCompletions();
    }

    private void setAtChatCompletions() {
        Bukkit.getOnlinePlayers().forEach(player -> player.setCustomChatCompletions(Bukkit.getOnlinePlayers().stream().map(player1 -> "@" + player1.getName()).toList()));
    }

    @EventHandler
    public void avoidDamage(EntityDamageEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION || game == null)
            event.setCancelled(true);
    }

    //    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (!Game.isRunning() || game.isDebugging) return;

        event.renderer((source, sourceDisplayName, message, viewer) -> {
            if (!(viewer instanceof Player player)) return Component.empty();

            var ps = PlayerData.of(source);
            var pv = PlayerData.of(player);
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

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        var player = event.getPlayer();

        if (players_looking_document.contains(player)) {
            player.setGameMode(GameMode.ADVENTURE);
            players_looking_document.remove(player);
        }

        if (game != null && game.isPaused) event.setCancelled(true);
        if (!Game.isRunning()) return;

        var pd = PlayerData.of(player);
        if (pd.hasEffect(Giddy.class)) {
            event.setCancelled(true);
            return;
        } else if (!pd.canMove()) {
            player.teleport(event.getFrom().setDirection(event.getTo().getDirection()));
            event.setCancelled(true);
            return;
        }

        pd.generator_tick = -1;

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
        playerStoredData.remove(player);
        players_looking_document.remove(player);
        prepared_players.remove(player);
        if (Game.isRunning()) {
            if (game.playerData.containsKey(player)) {
                game.pause();
            }
        } else if (game != null && game.chooseRole != null) {
            game.end(null);
        }
        setAtChatCompletions();
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
    public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {
        if (game == null || game.chooseRole != null) return;
        if (event.getEntity() instanceof Player player && event.getDamager() instanceof Player damager) {
            var pd_damager = PlayerData.of(damager);
            var pd_hurt = PlayerData.of(player);

            if (pd_damager.getRole() instanceof Hunter h) {
                if (!h.canAttack()) {
                    event.setCancelled(true);
                    return;
                }
                if (pd_damager.hasEffect(Invisibility.class)) pd_damager.removeEffect(Invisibility.class);
                pd_hurt.damageOrHeal(1);
                h.attack();
            } else {
                event.setCancelled(true);
            }

            event.setDamage(0);
        }
    }

    @EventHandler
    void onToggleSnake(PlayerToggleSneakEvent event) {
        if (Game.isStarted() && game.isPaused) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    void onAttack(PlayerArmSwingEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND || !Game.isRunning()) return;
        var player = event.getPlayer();
        var pd = PlayerData.of(player);
        if (pd == null || !(pd.getRole() instanceof Hunter h)) return;
        pd.addEffect(new Slowness(player, 20, 2));
        h.attack();
    }

    @EventHandler
    public void onInteractWithPlayer(PlayerInteractEntityEvent event) {
        if (Game.isRunning() && event.getRightClicked() instanceof Player clicked) {
            var pd = game.playerData.get(event.getPlayer());
            if (pd.hasEffect(Giddy.class)) {
                event.setCancelled(true);
            }
            var pd_clicked = game.playerData.get(clicked);
            if (pd_clicked.getRole() instanceof Survivor survivor && pd.getRole() instanceof Hunter hunter) {
                if (survivor.isDown()) {
                    hunter.addCaptured(clicked);
                }
            }
        }
    }
}
