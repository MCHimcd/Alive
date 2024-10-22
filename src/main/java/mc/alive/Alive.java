package mc.alive;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import mc.alive.api.Registries;
import mc.alive.game.Game;
import mc.alive.game.item.ChamberStandardCartridge;
import mc.alive.game.item.usable.gun.CabinGuardian;
import mc.alive.game.item.usable.gun.ChamberPistol;
import mc.alive.game.item.usable.gun.ChamberShotgun;
import mc.alive.listener.*;
import mc.alive.tick.TickRunner;
import mc.alive.util.Message;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;

import static mc.alive.game.Game.team_hunter;
import static mc.alive.game.Game.team_survivor;
import static org.bukkit.Bukkit.*;

public final class Alive extends JavaPlugin implements Listener {
    public static Alive plugin;
    public static Scoreboard main_scoreboard;
    public static YamlConfiguration config;

    @Override
    public void onDisable() {
        if (Game.game == null) return;
        Game.game.destroy();
    }

    @Override
    public void onEnable() {
        plugin = this;
        config = (YamlConfiguration) getConfig();
        saveConfig();
        initScoreboard();
        registerCommands();
        registerListeners();
        registerGameItems();
        new TickRunner().runTaskTimer(this, 0, 1);
        getOnlinePlayers().forEach(Game::resetPlayer);
    }

    private void registerGameItems() {
        List.of(
                ChamberStandardCartridge.class,
                CabinGuardian.class,
                ChamberPistol.class,
                ChamberShotgun.class
        ).forEach(Registries::registerGameItem);
    }

    private void registerListeners() {
        List.of(
                this,
                new ItemListener(),
                new PlayerListener(),
                new GunListener(),
                new MechanismListener(),
                new StaminaListener()
        ).forEach(l -> getPluginManager().registerEvents(l, plugin));
    }

    private void initScoreboard() {
        main_scoreboard = getScoreboardManager().getMainScoreboard();

        team_hunter = main_scoreboard.getTeam("hunter");
        if (team_hunter == null) {
            team_hunter = main_scoreboard.registerNewTeam("hunter");
            team_hunter.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            team_hunter.color(NamedTextColor.DARK_PURPLE);
        }

        team_survivor = main_scoreboard.getTeam("survivor");
        if (team_survivor == null) {
            team_survivor = main_scoreboard.registerNewTeam("survivor");
            team_survivor.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS);
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
                                if (ctx.getSource().getSender() instanceof Player && Game.game != null) {
                                    Game.game.end();
                                    Game.game = null;
                                    getOnlinePlayers().forEach(Game::resetPlayer);
                                }
                                return Command.SINGLE_SUCCESS;
                            }).build(),
                    "重置游戏",
                    List.of("ar")
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
                broadcast(Message.rMsg("<%s> <aqua>%s".formatted(event.getPlayer().getName(), player.getName())));
            }
        });
    }
}
