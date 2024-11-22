package mc.alive.mechanism;

import mc.alive.Game;
import mc.alive.PlayerData;
import mc.alive.role.hunter.Hunter;
import mc.alive.tick.TickRunnable;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static mc.alive.Game.game;

public class GhostDom implements TickRunnable {
    private final List<Player> players = new LinkedList<>();
    private final Location location;
    private int save_tick = 0;

    private GhostDom(Location loc) {
        location = loc;
        startTick();
    }

    public static void summon() {
        //todo生成鬼界
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    @Override
    public void tick() {
        if (!Game.isRunning()) return;
        AtomicBoolean has_player = new AtomicBoolean(false);
        location.getNearbyPlayers(1).forEach(player -> {
            if (game.hunter.equals(player)) {
                ((Hunter) PlayerData.of(player).getRole()).sealCaptured(this);
                return;
            }
            if (player.isSneaking()) {
                has_player.set(true);
                if (++save_tick == 60) {
                    players.forEach(pl -> PlayerData.of(pl).damageOrHeal(-1));
                    players.clear();
                }
            }
        });
        if (!has_player.get()) save_tick = 0;
    }
}
