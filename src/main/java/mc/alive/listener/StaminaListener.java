package mc.alive.listener;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import mc.alive.game.Game;
import mc.alive.game.PlayerData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSprintEvent;

public class StaminaListener implements Listener {
    @EventHandler
    public void onJump(PlayerJumpEvent event) {
        if (!Game.isStarted()) return;
        var pd = PlayerData.getPlayerData(event.getPlayer());
        if (!pd.addStamina(-25)) event.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerToggleSprintEvent event) {
        if (!Game.isStarted()) return;
        var pd = PlayerData.getPlayerData(event.getPlayer());
        if (!pd.addStamina(0)) event.setCancelled(true);
    }
}
