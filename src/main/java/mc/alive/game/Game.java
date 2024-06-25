package mc.alive.game;

import mc.alive.menu.MainMenu;
import mc.alive.util.ItemCreator;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


public class Game {
    public final Map<Player, PlayerData> playerData = new HashMap<>();
    public final Map<ItemDisplay, Integer> roles = new HashMap<>();
    public final List<Player> sailors;
    public final Player butcher;
    private final List<Player> choosing = new ArrayList<>();
    public Player currentPlayer;


    public Game(List<Player> players) {
        MainMenu.prepared.clear();
        choosing.addAll(players);
        players.forEach(player -> player.getInventory().clear());
        butcher = players.removeFirst();
        sailors = players;
        nextChoose();
    }

    private void start() {
        playerData.get(butcher).getRole().equip();
        for (Player player : sailors) {
            playerData.get(player).getRole().equip();
        }
        Bukkit.broadcast(Component.text("start"));
    }

    private void summonItemDisplay(boolean isButcher) {
        var world = Bukkit.getWorld("world");
        assert world != null;
        roles.clear();
        if (isButcher) {
            world.spawn(new Location(world, -2, 99, -4), ItemDisplay.class, id -> {
                id.setItemStack(ItemCreator.create(Material.DIAMOND_HOE,100).getItem());
                id.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
                var t=id.getTransformation();
                id.setTransformation(new Transformation(t.getTranslation().add(0,-.5f,0),t.getLeftRotation(),t.getScale(),t.getRightRotation()));
                roles.put(id, 100);
            });
        } else {
            world.spawn(new Location(world, -2, 99, -4), ItemDisplay.class, id -> {
                id.setItemStack(ItemCreator.create(Material.DIAMOND,200).getItem());
                var t=id.getTransformation();
                id.setTransformation(new Transformation(t.getTranslation().add(0,-.5f,0),t.getLeftRotation(),t.getScale(),t.getRightRotation()));
                id.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
                roles.put(id, 200);
            });
        }
    }

    public void nextChoose() {
        var world = Bukkit.getWorld("world");
        assert world != null;
        //上一个
        if(currentPlayer != null) {
            if(currentPlayer.equals(butcher)){
                roles.keySet().forEach(Entity::remove);
            }
            currentPlayer.teleport(new Location(world, 0.5, 97, 0.5));
        }
        if (choosing.isEmpty()) {
            roles.keySet().forEach(Entity::remove);
            start();
            return;
        }
        //下一个
        currentPlayer = choosing.removeFirst();
        summonItemDisplay(currentPlayer.equals(butcher));
        currentPlayer.teleport(new Location(world, 0.5, 99, 0.5));
    }

    public void destroy() {
        roles.keySet().forEach(Entity::remove);
    }
}
