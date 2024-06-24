package mc.alive.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MainMenu extends SlotMenu {
    public static final List<Player> prepared = new ArrayList<>();

    public MainMenu(Player p) {
        super(27, Component.text("主菜单", NamedTextColor.GOLD), p);
        //资料
        setSlot(11, new ItemStack(Material.BOOK), (it, pl) -> {
        });
        //加入游戏
        update(p);
        if(p.isOp()){
            setSlot(26, new ItemStack(Material.ARROW), (it, pl) -> {

            });
        }
    }

    private void update(Player p) {
        if (prepared.contains(p)) setSlot(15, new ItemStack(Material.CRYING_OBSIDIAN), (it, pl) -> {
            prepared.remove(p);
            update(p);
            close=false;
        });
        else setSlot(15, new ItemStack(Material.OBSIDIAN), (it, pl) -> {
            prepared.add(p);
            update(p);
            close=false;
        });
    }
}
