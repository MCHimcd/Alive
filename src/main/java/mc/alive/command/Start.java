package mc.alive.command;

import mc.alive.Alive;
import mc.alive.game.Game;
import mc.alive.game.PlayerData;
import mc.alive.util.ItemCreator;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Creature;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static mc.alive.Alive.game;
import static mc.alive.Alive.plugin;

public class Start implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        List<Player> l = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(l);
        game.destroy();
        game = new Game(l);
        return true;
    }
}
