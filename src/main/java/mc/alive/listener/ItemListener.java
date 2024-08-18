package mc.alive.listener;

import mc.alive.game.Game;
import mc.alive.game.PlayerData;
import mc.alive.game.effect.Giddy;
import mc.alive.game.item.PickUp;
import mc.alive.game.role.hunter.Hunter;
import mc.alive.game.role.survivor.Survivor;
import mc.alive.menu.MainMenu;
import mc.alive.util.ItemCheck;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

import static mc.alive.game.PlayerData.getPlayerData;
import static mc.alive.game.item.PickUp.*;
import static mc.alive.tick.PlayerTickrunnable.chosen_item_display;
import static mc.alive.util.Message.rMsg;

public class ItemListener implements Listener {

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (Game.isStarted()) {
            var pd = Game.instance.playerData.get(event.getPlayer());
            ItemStack item = event.getOffHandItem();

            if (ItemCheck.hasCustomModelData(item)) {
                var data = item.getItemMeta().getCustomModelData();

                if (ItemCheck.isSkill(data)) {
                    //技能
                    PlayerData.getPlayerData(event.getPlayer()).changeSkillValue();
                    event.setCancelled(true);
                } else if (ItemCheck.isGun(data) && getPlayerData(event.getPlayer()).getRole() instanceof Survivor) {
                    //枪
                    Game.instance.guns.get(item).reload(event.getPlayer());
                    event.setCancelled(true);
                }
            }

            if (pd.hasEffect(Giddy.class)) {
                event.setCancelled(true);
            }
        }

        ItemStack item = event.getOffHandItem();
        if (item.hasItemMeta() && item.getItemMeta().getCustomModelData() == 20000 /*主菜单*/) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickUp(PlayerAttemptPickupItemEvent event) {
        if (!Game.isStarted()) return;
        var item = event.getItem();
        if (item.isOnGround() && Game.instance.item_onground.containsKey(item)) {
            var pickUp = Game.instance.item_onground.get(item);
            var npl = item.getWorld().getNearbyPlayers(item.getLocation(), 1,
                    pl -> switch (pickUp) {
                        case BOTH -> true;
                        case HUNTER -> getPlayerData(pl).getRole() instanceof Hunter;
                        case SURVIVOR -> getPlayerData(pl).getRole() instanceof Survivor;
                    }).stream().findAny();
            if (npl.isPresent()) {
                item.setPickupDelay(0);
                item.setOwner(npl.get().getUniqueId());
                Game.instance.item_onground.remove(item);
            } else {
                item.setOwner(new UUID(0, 0));
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        ItemStack item = event.getItem();
        Player player = event.getPlayer();

        if (Game.instance != null) {
            if (Game.instance.chooseRole != null && Game.instance.chooseRole.handleEvent(player)) return;
            if (Game.instance.chooseRole != null) return;
            var pd = Game.instance.playerData.get(player);
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
                !ItemCheck.hasCustomModelData(item)
                        || event.getAction() == Action.LEFT_CLICK_AIR
                        || event.getAction() == Action.LEFT_CLICK_BLOCK
        ) return;

        //使用物品
        if (Game.instance != null) {
            var data = item.getItemMeta().getCustomModelData();

            if (ItemCheck.isSkill(data)) {
                //技能
                PlayerData.getPlayerData(player).useSkill();
                event.setCancelled(true);
            } else if (ItemCheck.isGun(data) && getPlayerData(player).getRole() instanceof Survivor) {
                //枪
                Game.instance.guns.get(item).startShoot(player);
            }
        } else {
            switch (item.getItemMeta().getCustomModelData()) {
                case 20000 -> {
                    //主菜单
                    player.openInventory(new MainMenu(player).getInventory());
                    player.playSound(player, Sound.BLOCK_BARREL_OPEN, 1, 1);
                }
                case 20001 -> {
                }
            }
        }

    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        var itemStack = event.getItemDrop().getItemStack();
        if (ItemCheck.hasCustomModelData(itemStack)) {
            var data = itemStack.getItemMeta().getCustomModelData();
            if (ItemCheck.isSkill(data) || data == 90000
            ) {
                event.setCancelled(true);
            } else if (ItemCheck.isPickable(data) && Game.isStarted()) {
                var item = event.getItemDrop();
                ItemStack is = item.getItemStack();
                item.customName(is.displayName().append(is.getAmount() == 1 ? Component.empty() : rMsg("*%d".formatted(is.getAmount()))));
                item.setCustomNameVisible(true);
                item.setCanMobPickup(false);
                item.setWillAge(false);
                item.setCanMobPickup(false);
                PickUp pickUp = BOTH;
                var pd = Game.instance.playerData.get(event.getPlayer());
                if (pd.getRole() instanceof Survivor) pickUp = SURVIVOR;
                else if (pd.getRole() instanceof Hunter) pickUp = HUNTER;
                Game.instance.item_onground.put(item, pickUp);
            }
        }
    }
}
