package mc.alive;

import mc.alive.command.Start;
import mc.alive.game.Game;
import mc.alive.game.PlayerData;
import mc.alive.game.TickRunner;
import mc.alive.menu.MainMenu;
import mc.alive.menu.SlotMenu;
import mc.alive.role.Role;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class Alive extends JavaPlugin implements Listener {
    public static Alive plugin;
    public static Game game = null;

    @Override
    public void onEnable() {
        plugin = this;
        Bukkit.getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(Bukkit.getPluginCommand("start")).setExecutor(new Start());
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
    public void onHurt(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(false);
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
        if (item == null) {
            if (game != null) {
                if (player.equals(game.currentPlayer)) {
                    //选role
                    var td = TickRunner.chosen.get(player);
                    if (td != null) {
                        var role = game.roles.get(td);
                        if (role != null) {
                            game.playerData.put(player, new PlayerData(player, Role.of(role, player)));
                            Bukkit.broadcast(Component.text(game.playerData.size()));
                            game.nextChoose();
                        }
                    }
                }
            }
            return;
        }
        if (
                !item.hasItemMeta()
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
        var item1 = event.getCurrentItem();
        if (event.getRawSlot() > event.getInventory().getSize()) {
            if (item1 != null) {
                if (item1.hasItemMeta()) {
                    switch (item1.getItemMeta().getCustomModelData()) {
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
//        if (game != null &&
//                (
//                        item.hasItemMeta()
//                                && item.getItemMeta().hasCustomModelData()
//                                && item.getItemMeta().getCustomModelData() == 10000
//                ) ||
//                (
//                        item1 != null
//                                && item1.hasItemMeta()
//                                && item1.getItemMeta().hasCustomModelData()
//                                && item1.getItemMeta().getCustomModelData() == 10000
//                )
//        ) {
//            event.setCancelled(true);
//        }
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
        if (game == null) return;
        var player = event.getPlayer();
//        if (player.equals(game.currentPlayer)) {
//            var l=player.getLocation();
//            var to=event.getTo();
//            l.setDirection(to.getDirection());
//            player.teleport(l);
//        }
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
}
