package mc.alive.mechanism;

import mc.alive.Game;
import mc.alive.PlayerData;
import mc.alive.role.hunter.Hunter;
import mc.alive.role.survivor.Survivor;
import mc.alive.tick.TickRunnable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
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
        new GhostDom(new Location(Bukkit.getWorld("world"), -4.5, -60, 4.5));
    }

    public Location getLocation() {
        return location;
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
            Survivor survivor = (Survivor) PlayerData.of(player).getRole();
            if (player.isSneaking() && !survivor.isSealed() && !survivor.isDown()) {
                has_player.set(true);
                if (++save_tick == 60) {
                    players.forEach(pl -> {
                        PlayerData playerData = PlayerData.of(pl);
                        playerData.damageOrHeal(-1);
                    });
                    players.clear();
                }
            }
        });
        if (!has_player.get()) save_tick = 0;
        location.getWorld().spawnParticle(Particle.END_ROD, location, 1);
    }
}
