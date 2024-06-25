package mc.alive;

import mc.alive.command.Start;
import mc.alive.game.Game;
import mc.alive.game.PlayerData;
import mc.alive.game.TickRunner;
import mc.alive.menu.MainMenu;
import mc.alive.menu.SlotMenu;
import mc.alive.util.ItemCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import javax.security.auth.kerberos.KerberosTicket;
import java.util.Collection;
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
    }

    @Override
    public void onDisable() {
        game.destroy();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
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
            getPlayerData(event.getPlayer()).changeSkillValue();
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        ItemStack item = event.getItem();
        if (item == null
                || !item.hasItemMeta()
                || !item.getItemMeta().hasCustomModelData()
                || event.getAction() == Action.LEFT_CLICK_AIR
                || event.getAction() == Action.LEFT_CLICK_BLOCK
        ) return;
        Player player = event.getPlayer();
        if (game != null) {
            switch (item.getItemMeta().getCustomModelData()) {
                case 10000 -> {
                    getPlayerData(player).useSkill();
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
            player.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f);
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!player.isOp()) event.setCancelled(true);
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

    private PlayerData getPlayerData(Player player) {
        return game.playerData.get(player);
    }
}
