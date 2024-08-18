package mc.alive;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import mc.alive.game.Game;
import mc.alive.game.item.gun.CabinGuardian;
import mc.alive.game.item.gun.ChamberPistol;
import mc.alive.game.item.gun.ChamberShotgun;
import mc.alive.listener.GunListener;
import mc.alive.listener.ItemListener;
import mc.alive.listener.PlayerListener;
import mc.alive.tick.TickRunner;
import mc.alive.util.ItemBuilder;
import mc.alive.util.Message;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
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

    @Override
    public void onDisable() {
        if (Game.instance == null) return;
        Game.instance.destroy();
    }

    @Override
    public void onEnable() {
        plugin = this;
        initScoreboard();
        registerCommands();
        registerListeners();
        new TickRunner().runTaskTimer(this, 0, 1);
        getOnlinePlayers().forEach(Game::resetPlayer);
    }

    private void registerListeners() {
        var manager = getPluginManager();
        manager.registerEvents(this, this);
        manager.registerEvents(new ItemListener(), this);
        manager.registerEvents(new PlayerListener(), this);
        manager.registerEvents(new GunListener(), this);
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
                                if (ctx.getSource().getSender() instanceof Player && Game.instance != null) {
                                    Game.instance.end();
                                    Game.instance = null;
                                    getOnlinePlayers().forEach(Game::resetPlayer);
                                }
                                return Command.SINGLE_SUCCESS;
                            }).build(),
                    "重置游戏",
                    List.of("ar")
            );
            cs.register(
                    Commands.literal("gun")
                            .executes(ctx -> {
                                if (ctx.getSource().getSender() instanceof Player pl && Game.instance != null) {
                                    var it = ItemBuilder.getGunItemStack(80000);
                                    Game.instance.guns.put(it, new ChamberPistol());
                                    pl.getInventory().addItem(it);
                                    var it2 = ItemBuilder.getGunItemStack(80001);
                                    Game.instance.guns.put(it2, new ChamberShotgun());
                                    pl.getInventory().addItem(it2);
                                    var it3 = ItemBuilder.getGunItemStack(80002);
                                    Game.instance.guns.put(it3, new CabinGuardian());
                                    pl.getInventory().addItem(it3);
                                    for (int i = 0; i < 5; i++) {
                                        pl.getInventory().addItem(
                                                ItemBuilder.material(Material.DIAMOND, 90001)
                                                        .name(Component.text("舱室标准弹"))
                                                        .amount(64)
                                                        .build()
                                        );
                                    }

                                }
                                return Command.SINGLE_SUCCESS;
                            }).build(),
                    "枪",
                    List.of()
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
