package mc.alive;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import mc.alive.game.Game;
import mc.alive.game.PlayerData;
import mc.alive.game.TickRunner;
import mc.alive.menu.MainMenu;
import mc.alive.menu.SlotMenu;
import mc.alive.role.hunter.Hunter;
import mc.alive.util.ItemCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static mc.alive.game.PlayerData.getPlayerData;
import static mc.alive.game.TickRunner.chosen_item_display;
import static mc.alive.menu.MainMenu.doc;
import static mc.alive.menu.MainMenu.prepared;

public final class Alive extends JavaPlugin implements Listener {
    public static Alive plugin;
    public static Game game = null;

    @Override
    public void onEnable() {
        plugin = this;
        Bukkit.getPluginManager().registerEvents(this, this);
        registerCommands();
        new TickRunner().runTaskTimer(this, 0, 1);
        Bukkit.getOnlinePlayers().forEach(this::resetPlayer);
    }

    @Override
    public void onDisable() {
        if (game != null) {
            game.destroy();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        resetPlayer(event.getPlayer());
    }

    @EventHandler
    public void onHurt(EntityDamageByEntityEvent event) {
        if (game == null || game.chooseRole != null) return;
        if (event.getEntity() instanceof Player player && event.getDamager() instanceof Player damager) {
            var pdp = getPlayerData(player);
            var pdd = getPlayerData(damager);
            int damage = (int) (pdd.getRole().getStrength()* damager.getAttackCooldown());
            double damagedealt = event.isCritical() ? damage * 1.3 : damage;
            pdp.damage(damagedealt);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (game != null) {
            ItemStack item = event.getOffHandItem();
            if (!item.hasItemMeta() || item.getItemMeta().getCustomModelData() != 10000) return;
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
                return;
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
            switch (item.getItemMeta().getCustomModelData()) {
                case 10000 -> {
                    PlayerData.getPlayerData(player).useSkill();
                    event.setCancelled(true);
                }
                case 10001 -> {

                }
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
        if (!(event.getWhoClicked() instanceof Player player)) return;
        var item = event.getCurrentItem();
        if (event.getRawSlot() > event.getInventory().getSize()) {
            if (item != null) {
                if (item.hasItemMeta()) {
                    switch (item.getItemMeta().getCustomModelData()) {
                        case 10000 -> {
                            event.setCancelled(true);
                        }
                        case 20000 -> {
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
        if (!(event.getInventory().getHolder() instanceof SlotMenu menu)) return;
        menu.handleClick(event.getSlot());
        event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        int i = event.getItemDrop().getItemStack().getItemMeta().getCustomModelData();
        switch (i) {
            case 10000 -> event.setCancelled(true);
            case 20000 -> event.setCancelled(true);
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

    private void resetPlayer(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.clearActivePotionEffects();
        player.getInventory().clear();
        player.getInventory().setItem(8, ItemCreator
                .create(Material.CLOCK)
                .data(20000)
                .name(Component.text("主菜单", NamedTextColor.GOLD))
                .getItem()
        );
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
                                    Bukkit.getOnlinePlayers().forEach(this::resetPlayer);
                                }
                                return Command.SINGLE_SUCCESS;
                            }).build(),
                    "重置游戏",
                    List.of("ar")
            );
        });
    }
}
