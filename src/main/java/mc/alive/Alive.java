package mc.alive;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import mc.alive.item.DoorCard;
import mc.alive.item.GameItem;
import mc.alive.item.pickup.LevelUp;
import mc.alive.listener.ItemListener;
import mc.alive.listener.MechanismListener;
import mc.alive.listener.PlayerListener;
import mc.alive.tick.TickRunner;
import mc.alive.util.Message;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static mc.alive.Game.*;
import static mc.alive.util.Message.rMsg;
import static org.bukkit.Bukkit.*;

public final class Alive extends JavaPlugin implements Listener {
    public static Alive plugin;
    public static Scoreboard main_scoreboard;
    public static YamlConfiguration locations_config, roles_config;
    public static File data_folder, data_file;

    @Override
    public void onDisable() {
        try (FileOutputStream fileOut = new FileOutputStream(data_file); ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            StoredData.data.forEach(data -> {
                if (data.isValid()) try {
                    out.writeObject(data);
                } catch (IOException e) {
                    getLogger().warning(e.getLocalizedMessage());
                }
            });
            getLogger().info("Serialized data is saved in data.ser");

        } catch (IOException i) {
            getLogger().warning(i.getLocalizedMessage());
        }

        if (Game.game == null) return;
        Game.game.destroy();
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public void onEnable() {
        locations_config = YamlConfiguration.loadConfiguration(new InputStreamReader(getResource("locations.yml"), StandardCharsets.UTF_8));
        roles_config = YamlConfiguration.loadConfiguration(new InputStreamReader(getResource("roles.yml"), StandardCharsets.UTF_8));
        data_folder = getDataFolder();
        data_file = new File(data_folder.getPath() + "/data.ser");

        plugin = this;
        initScoreboard();
        registerCommands();
        registerListeners();
        registerGameItems();

        new TickRunner().runTaskTimer(this, 0, 1);
        getOnlinePlayers().forEach(Game::resetPlayer);


        if (!data_folder.exists()) {
            if (!data_folder.mkdir()) getLogger().warning("Failed to create Alive directory");
        }
        if (!data_file.exists()) {
            try {
                if (!data_file.createNewFile()) getLogger().warning("Failed to create data.ser");
            } catch (IOException e) {
                getLogger().warning(e.getLocalizedMessage());
            }
        }
        try (FileInputStream fileIn = new FileInputStream(data_file); ObjectInputStream in = new ObjectInputStream(fileIn)) {
            while (true) {
                try {
                    StoredData readData = (StoredData) in.readObject();
                    if (!readData.isValid()) continue;
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        if (player.getName().equals(readData.getName())) {
                            StoredData.playerStoredData.put(player, readData);
                        }
                    });
                    StoredData.data.add(readData);
                } catch (EOFException e) {
                    break;
                }
            }

        } catch (Exception e) {
            getLogger().warning(e.getLocalizedMessage());
        }
    }

    private void registerGameItems() {
        List.of(
                LevelUp.class,
                DoorCard.class
        ).forEach(GameItem::register);
    }

    private void registerListeners() {
        List.of(
                this,
                new ItemListener(),
                new PlayerListener(),
                new MechanismListener()
        ).forEach(l -> getPluginManager().registerEvents(l, plugin));
    }

    private void initScoreboard() {
        main_scoreboard = getScoreboardManager().getMainScoreboard();

        team_hunter = main_scoreboard.getTeam("hunter");
        if (team_hunter == null) {
            team_hunter = main_scoreboard.registerNewTeam("hunter");
            team_hunter.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            team_hunter.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            team_hunter.color(NamedTextColor.DARK_PURPLE);
        }

        team_survivor = main_scoreboard.getTeam("survivor");
        if (team_survivor == null) {
            team_survivor = main_scoreboard.registerNewTeam("survivor");
            team_survivor.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS);
            team_survivor.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            team_survivor.color(NamedTextColor.DARK_GRAY);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private void registerCommands() {
        var manager = getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            var cs = event.registrar();
            cs.register(
                    Commands.literal("reset")
                            .executes(ctx -> {
                                if (ctx.getSource().getSender() instanceof Player player && Game.game != null) {
                                    Game.game.end(player);
                                    Game.game = null;
                                    getOnlinePlayers().forEach(Game::resetPlayer);
                                }
                                return Command.SINGLE_SUCCESS;
                            }).build(),
                    "重置游戏",
                    List.of("a_r")
            );
            cs.register(

                    Commands.literal("test")
                            .executes(ctx -> {
                                if (ctx.getSource().getSender() instanceof Player) {
                                    List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers().stream().limit(3).toList());
                                    game = new Game(players, true);
                                }
                                return Command.SINGLE_SUCCESS;
                            }).build(),
                    "测试",
                    List.of("a_t")
            );

        });
    }

    @EventHandler
    public void at(AsyncChatEvent event) {
        String messageString = Message.msg.serialize(event.message());
        getOnlinePlayers().forEach(player -> {
            String atString = "@%s".formatted(player.getName());
            if (messageString.equals(atString)) {
                Player atplayer = getPlayer(player.getName());
                if (atplayer != null) {
                    atplayer.showTitle(Message.title("", "<green>--<red><bold>你被@了</bold><green>--", 0, 1000, 0));
                    new BukkitRunnable() {
                        int time = 20;

                        @Override
                        public void run() {
                            atplayer.playSound(atplayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 10f, 10f);
                            atplayer.playSound(atplayer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 10f, 10f);
                            if (time-- <= 0) {
                                cancel();
                            }
                        }
                    }.runTaskTimer(plugin, 0, 1);
                }
                event.setCancelled(true);
                broadcast(rMsg("<%s> <aqua>%s".formatted(event.getPlayer().getName(), player.getName())));
            }
        });
    }
}
