package mc.alive.game;

import mc.alive.Alive;
import mc.alive.menu.MainMenu;
import mc.alive.util.ChooseRole;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static mc.alive.util.Message.rMsg;


public class Game {
    public final Map<Player, PlayerData> playerData = new HashMap<>();
    public final List<Player> survivors;
    public final Player hunter;
    public ChooseRole chooseRole;
    private final List<Entity> markers = new LinkedList<>();
    private final Map<ItemDisplay, Integer> fix_progress = new HashMap<>();


    public Game(List<Player> players) {
        MainMenu.prepared.clear();
        chooseRole = new ChooseRole(players);
        players.forEach(player -> {
            player.getInventory().clear();
            player.closeInventory();
        });
        hunter = players.removeFirst();
        survivors = players;
        new BukkitRunnable() {
            @Override
            public void run() {

                chooseRole.nextChoose();
            }
        }.runTaskLater(Alive.plugin, 1);
    }

    public void start() {
        chooseRole.roles.keySet().forEach(Entity::remove);
        chooseRole = null;
        playerData.get(hunter).getRole().equip();
        Objects.requireNonNull(hunter.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(playerData.get(hunter).getRole().getSpeed() * 0.1);
        for (Player player : survivors) {
            playerData.get(player).getRole().equip();
            //设置属性
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(playerData.get(player).getRole().getSpeed() * 0.1);
        }
        summonEntities();
        Bukkit.broadcast(Component.text("start"));
    }

    public void end() {
        destroy();
        Alive.game = null;
    }

    public void destroy() {
        if (chooseRole!=null){
            chooseRole.roles.keySet().forEach(Entity::remove);
        }
        markers.forEach(Entity::remove);
        fix_progress.keySet().forEach(Entity::remove);
    }

    private void summonEntities() {
        //管道入口
        for (var s : new String[]{
                "1.5 -59 1.5"
        }) {
            var xyz = Arrays.stream(s.split(" ")).mapToDouble(Double::parseDouble).toArray();
            var world = Bukkit.getWorld("world");
            assert world != null;
            world.spawn(new Location(world, xyz[0], xyz[1], xyz[2]), Marker.class, markers::add);
        }
        //维修
        for (var s : new String[]{
                "1.5 -59 1.5"
        }) {
            var xyz = Arrays.stream(s.split(" ")).mapToDouble(Double::parseDouble).toArray();
            var world = Bukkit.getWorld("world");
            assert world != null;
            world.spawn(new Location(world, xyz[0], xyz[1], xyz[2]), ItemDisplay.class, id -> {
                id.setItemStack(new ItemStack(Material.FEATHER));
                fix_progress.put(id, 0);
            });
        }
    }

    public int fix(ItemDisplay id, int amount) {
        if (amount == 0) return fix_progress.get(id);
        var final_amount = fix_progress.get(id) + amount;
        if (final_amount >= 400) {
            fix_progress.remove(id);
            id.remove();
            Bukkit.broadcast(rMsg("fix complete"));
        } else {
            fix_progress.put(id, final_amount);
        }
        return final_amount;
    }
}