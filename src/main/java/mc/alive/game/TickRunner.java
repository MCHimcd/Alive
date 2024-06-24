package mc.alive.game;

import org.bukkit.scheduler.BukkitRunnable;

import static mc.alive.Alive.game;

public class TickRunner extends BukkitRunnable {
    @Override
    public void run() {
        if(game!=null) game.playerData.values().forEach(PlayerData::tick);
    }
}
