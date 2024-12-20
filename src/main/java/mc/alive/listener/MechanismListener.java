package mc.alive.listener;

import mc.alive.Game;
import mc.alive.PlayerData;
import mc.alive.effect.Slowness;
import mc.alive.mechanism.Barrier;
import mc.alive.mechanism.Door;
import mc.alive.mechanism.Lift;
import mc.alive.mechanism.LiftDoor;
import mc.alive.menu.LiftMenu;
import mc.alive.role.hunter.Hunter;
import mc.alive.role.survivor.Survivor;
import mc.alive.tick.MechanismTickrunnable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;

import static mc.alive.Game.game;

public class MechanismListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!Game.isRunning()) return;
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        var action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        var role = PlayerData.of(player).getRole();

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

        //开门
        Door door = MechanismTickrunnable.chosenDoors.get(player);
        if (door != null && !player.equals(game.hunter)) {
            door.action(player);
        }

        //板子
        Barrier barrier = Barrier.chosenBarriers.get(player);
        if (barrier != null) {
            barrier.trigger(role instanceof Hunter, player);
        }

        if (action == Action.RIGHT_CLICK_BLOCK) {
            var block = event.getClickedBlock();
            //呼叫电梯
            LiftDoor liftDoor = game.liftDoors.get(block);
            if (liftDoor != null && role instanceof Survivor) {
                liftDoor.callLift();
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!Game.isRunning()) return;
        game.barriers.forEach((_, b) -> {
            if (b.isTriggered()
                    && b.getBoundingBox().contains(event.getTo().toVector())
                    && !b.getBoundingBox().contains(event.getFrom().toVector())
            ) {
                Player player = event.getPlayer();
                if (player.equals(game.hunter)) event.setCancelled(true);
                else PlayerData.of(player).addEffect(new Slowness(player, 5, 7));
            }
        });
    }
}
