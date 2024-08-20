package mc.alive.listener;

import mc.alive.game.Game;
import mc.alive.game.PlayerData;
import mc.alive.game.mechanism.Lift;
import mc.alive.game.mechanism.LiftDoor;
import mc.alive.game.role.hunter.Hunter;
import mc.alive.game.role.survivor.Survivor;
import mc.alive.menu.LiftMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import static mc.alive.game.Game.game;

public class MechanismListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!Game.isStarted()) return;
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        var action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        var role = PlayerData.getPlayerData(player).getRole();

        //电梯
        for (Lift lift : game.lifts.values()) {
            if (lift.players.contains(player)) {
                if (role instanceof Survivor) {
                    event.setCancelled(true);
                    player.openInventory(new LiftMenu(player, lift).getInventory());
                }
                break;
            }
        }

        //呼叫电梯
        if (action == Action.RIGHT_CLICK_BLOCK) {
            var block = event.getClickedBlock();
            LiftDoor liftDoor = game.liftDoors.get(block);
            if (liftDoor == null || role instanceof Hunter) return;
            liftDoor.callLift();
        }

    }
}
