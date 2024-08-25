package mc.alive.listener;

import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import mc.alive.Game;
import mc.alive.item.usable.gun.Gun;
import mc.alive.util.ItemCheck;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;

public class GunListener implements Listener {
    @EventHandler
    public void onStopUsing(PlayerStopUsingItemEvent event) {
        if (!Game.isStarted()) return;
        var player = event.getPlayer();
        var gun = Game.game.usable_items.get(player.getInventory().getItemInMainHand());
        if (gun != null) {
            ((Gun) gun).stopShoot(player);
        }
    }

    @EventHandler
    public void avoidConsume(PlayerItemConsumeEvent event) {
        var item = event.getItem();
        if (item.getType() == Material.HONEY_BOTTLE) event.setCancelled(true);
    }

    @EventHandler
    public void onChangeMainHandItem(PlayerItemHeldEvent event) {
        if (!Game.isStarted()) return;
        var player = event.getPlayer();
        //noinspection DataFlowIssue
        player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(255);
        var pre_it = player.getInventory().getItem(event.getPreviousSlot());
        if (!ItemCheck.hasCustomModelData(pre_it)) return;
        var pre_data = pre_it.getItemMeta().getCustomModelData();
        if (ItemCheck.isGun(pre_data)) {
            ((Gun) Game.game.usable_items.get(pre_it)).handleItemChange(player, true);
        }
        var new_it = player.getInventory().getItem(event.getNewSlot());
        if (!ItemCheck.hasCustomModelData(new_it)) return;
        var new_data = new_it.getItemMeta().getCustomModelData();
        if (ItemCheck.isGun(new_data)) {
            ((Gun) Game.game.usable_items.get(new_it)).handleItemChange(player, false);
        }
    }
}
