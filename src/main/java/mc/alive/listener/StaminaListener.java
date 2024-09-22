package mc.alive.listener;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import mc.alive.Game;
import mc.alive.PlayerData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class StaminaListener implements Listener {
    @EventHandler
    public void onJump(PlayerJumpEvent event) {
        if (!Game.isStarted()) return;
        if (Game.game.isPaused) {
            event.setCancelled(true);
            return;
        }
        var pd = PlayerData.of(event.getPlayer());
        var cost = 25 * 4 / pd.getRole().getStrength();
        if (pd.getStamina() < cost) {
            event.setCancelled(true);
            return;
        }
        pd.addStamina(-cost);
    }

}
