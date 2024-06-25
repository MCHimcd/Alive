package mc.alive.menu;

import mc.alive.game.Game;
import mc.alive.util.ItemCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static mc.alive.Alive.game;

public class MainMenu extends SlotMenu {
    public static final List<Player> prepared = new ArrayList<>();

    public MainMenu(Player p) {
        super(27, Component.text("主菜单", NamedTextColor.GOLD), p);
        //资料
        setSlot(11, new ItemStack(Material.BOOK), (it, pl) -> {
        });
        //加入游戏
        update(p);
    }

    private void update(Player p) {
        if (prepared.contains(p)) setSlot(15, new ItemStack(Material.CRYING_OBSIDIAN), (it, pl) -> {
            prepared.remove(p);
            update(p);
            close = false;
        });
        else setSlot(15, new ItemStack(Material.OBSIDIAN), (it, pl) -> {
            prepared.add(p);
            update(p);
            close = false;
        });
        if (p.isOp() && prepared.size() >= 2) {
            setSlot(26, ItemCreator.create(Material.NETHER_STAR).getItem(), (it, pl) -> {
                List<Player> l = new ArrayList<>(prepared);
                Collections.shuffle(l);
                if (game != null) game.destroy();
                game = new Game(l);
            });
        } else setSlot(26, ItemCreator.create(Material.BARRIER).getItem(), (it, pl) -> {
            close = false;
        });
    }
}
