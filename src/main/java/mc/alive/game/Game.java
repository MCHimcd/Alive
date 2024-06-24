package mc.alive.game;

import mc.alive.menu.MainMenu;
import mc.alive.role.Butchers.Hunter;
import mc.alive.role.Sailors.New;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static mc.alive.Alive.*;


public class Game {
    public final Map<Player, PlayerData> playerData = new HashMap<>();

    public Game(List<Player> players) {
        MainMenu.prepared.clear();
        var p = players.removeFirst();
        playerData.put(p, new PlayerData(p, new Hunter(p)));
        for (Player player : players) {
            playerData.put(player, new PlayerData(player, new New(player)));
        }
    }

    public void destroy() {

    }
}
