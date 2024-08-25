package mc.alive.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import mc.alive.Game;
import mc.alive.PlayerData;
import mc.alive.effect.Giddy;
import mc.alive.menu.SlotMenu;
import mc.alive.role.hunter.Hunter;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import static mc.alive.PlayerData.of;
import static mc.alive.menu.MainMenu.players_looking_document;
import static mc.alive.menu.MainMenu.prepared_players;

public class PlayerListener implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Game.resetPlayer(event.getPlayer());
        setAtChatCompletions();
    }

    private void setAtChatCompletions() {
        Bukkit.getOnlinePlayers().forEach(player -> player.setCustomChatCompletions(Bukkit.getOnlinePlayers().stream().map(player1 -> "@" + player1.getName()).toList()));
    }

    @EventHandler
    public void avoidDamage(EntityDamageEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION || Game.game == null)
            event.setCancelled(true);
    }

    //    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (Game.game == null) return;

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

        if (!Game.isStarted()) return;

        var pd = of(player);
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
        players_looking_document.remove(player);
        prepared_players.remove(player);
        if (Game.game != null) {
            Game.game.end();
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
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (Game.game == null || Game.game.chooseRole != null) return;
        if (event.getEntity() instanceof Player player && event.getDamager() instanceof Player damager) {
            var pd_hurt = of(player);
            var pd_damager = of(damager);
            if (pd_damager.attack_cd > 0) {
                event.setCancelled(true);
                return;
            }
            pd_damager.attack_cd = pd_damager.getRole().getAttackCD();
            if (pd_damager.getRole() instanceof Hunter) {
                pd_hurt.damageOrHeal(pd_damager.getRole().getStrength());
                pd_damager.addStamina(-50);
            } else {
                event.setCancelled(true);
            }
            event.setDamage(0);
        }
    }
}
