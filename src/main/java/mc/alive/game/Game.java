package mc.alive.game;

import mc.alive.Alive;
import mc.alive.menu.MainMenu;
import mc.alive.util.ChooseRole;
import mc.alive.util.ItemCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import java.util.*;

import static mc.alive.Alive.*;
import static mc.alive.util.Message.rMsg;
import static org.bukkit.attribute.Attribute.*;


public class Game {
    public final Map<Player, PlayerData> playerData = new HashMap<>();
    public final List<Player> survivors;
    public final Player hunter;
    public ChooseRole chooseRole;
    private final List<Entity> markers = new LinkedList<>();
    private final Map<ItemDisplay, Integer> fix_progress = new HashMap<>();
    public static Team t_hunter;
    public static Team t_survivor;


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
        }.runTaskLater(plugin, 1);
    }

    public static void resetPlayer(Player player) {
        Map.of(
                GENERIC_MOVEMENT_SPEED, .1,
                GENERIC_ATTACK_DAMAGE, 1.0,
                GENERIC_MAX_ABSORPTION, 20.0,
                GENERIC_ATTACK_SPEED, 255.0,
                GENERIC_ATTACK_KNOCKBACK, -1.0,
                GENERIC_JUMP_STRENGTH, .0
        ).forEach((key, value) -> {
            var a = player.getAttribute(key);
            assert a != null;
            a.setBaseValue(value);
        });
        var team = ms.getPlayerTeam(player);
        if (team != null) {
            team.removePlayer(player);
        }
        player.playerListName(player.name());
        player.displayName(player.name());
        player.teleport(new Location(player.getWorld(), -7.5, -59, 11.5));
        player.setHealth(20);
        player.setAbsorptionAmount(0);
        player.setFoodLevel(20);
        player.setGameMode(GameMode.ADVENTURE);
        player.clearActivePotionEffects();
        player.getInventory().clear();
        player.setCustomChatCompletions(Bukkit.getOnlinePlayers().stream().map(player1 -> "@" + player1.getName()).toList());
        if (game == null) player.getInventory().setItem(8, ItemCreator
                .create(Material.CLOCK)
                .data(20000)
                .name(Component.text("主菜单", NamedTextColor.GOLD))
                .getItem()
        );
    }

    public void start() {
        chooseRole.roles.keySet().forEach(Entity::remove);
        chooseRole = null;
        playerData.forEach((player, playerData1) -> {
            player.clearActivePotionEffects();
            player.setHealth(20);
            playerData1.getRole().equip();
        });
        summonEntities();
        Bukkit.broadcast(Component.text("start"));
    }

    public void end() {
        destroy();
        Alive.game = null;
        playerData.keySet().forEach(Game::resetPlayer);
        Bukkit.broadcast(Component.text("end"));
    }

    public void destroy() {
        if (chooseRole != null) {
            chooseRole.roles.keySet().forEach(Entity::remove);
        }
        markers.forEach(Entity::remove);
        fix_progress.keySet().forEach(Entity::remove);
    }

    private void summonEntities() {
        //管道入口
        for (var s : new String[]{
                "1.5 -58 1.5"
        }) {
            var xyz = Arrays.stream(s.split(" ")).mapToDouble(Double::parseDouble).toArray();
            var world = Bukkit.getWorld("world");
            assert world != null;
            world.spawn(new Location(world, xyz[0], xyz[1], xyz[2]), Marker.class, markers::add);
        }
        //维修
        for (var s : new String[]{
                "5.5 -59 5.5"
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