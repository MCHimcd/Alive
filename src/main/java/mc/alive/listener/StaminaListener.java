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
        var pd = PlayerData.of(event.getPlayer());
        if (pd.getStamina() < 25) {
            event.setCancelled(true);
            return;
        }
        pd.addStamina(-25);
    }

}
