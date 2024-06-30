package mc.alive.menu;

import mc.alive.game.Game;
import mc.alive.util.ItemCreator;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static mc.alive.Alive.game;
import static mc.alive.util.Message.convertMsg;
import static mc.alive.util.Message.rMsg;

public class MainMenu extends SlotMenu {
    public static final List<Player> prepared = new ArrayList<>();
    public static final List<Player> doc = new ArrayList<>();

    public MainMenu(Player p) {
        super(27, Component.text("主菜单", NamedTextColor.GOLD), p);
        //资料
        setSlot(11, ItemCreator.create(Material.BOOK).name(rMsg("资料", NamedTextColor.AQUA)).getItem(), (it, pl) -> {
            pl.setGameMode(GameMode.SPECTATOR);
            doc.add(pl);
            pl.closeInventory();
            pl.openBook(Book.builder().pages(convertMsg(List.of(
                    "<blue>document"
            ))).build());
            pl.teleport(pl);
            close = false;
        });
        //加入游戏
        if (prepared.contains(p)) {
            setSlot(15, ItemCreator.create(Material.CRYING_OBSIDIAN)
                    .name(rMsg("点击取消准备", NamedTextColor.RED))
                    .lore(rMsg("已准备人数：%d".formatted(prepared.size()), NamedTextColor.GRAY))
                    .getItem(), (it, pl) -> {
                //取消准备
                prepared.remove(pl);
                update();
                close = false;
            });
        } else
            setSlot(15, ItemCreator.create(Material.OBSIDIAN).name(rMsg("点击准备", NamedTextColor.GREEN)).getItem(), (it, pl) -> {
                //准备
                prepared.add(pl);
                update();
                close = false;
            });
        if (p.isOp()) {
            if (prepared.size() >= 2) {
                setSlot(26, ItemCreator.create(Material.NETHER_STAR).name(rMsg("开始游戏", NamedTextColor.GOLD)).getItem(), (it, pl) -> {
                    List<Player> l = new ArrayList<>(prepared);
                    Collections.shuffle(l);
                    if (game != null) game.destroy();
                    game = new Game(l);
                });
            } else setSlot(
                    26,
                    ItemCreator.create(Material.BARRIER).name(rMsg("人数不足", NamedTextColor.DARK_RED)).getItem(),
                    (it, pl) -> close = false
            );
        }
    }

    private void update() {
        Bukkit.getOnlinePlayers().forEach(player1 -> {
            if (player1.getOpenInventory().getTopInventory().getHolder() instanceof MainMenu m) {
                if (prepared.contains(player1)) {
                    m.setSlot(15, ItemCreator.create(Material.CRYING_OBSIDIAN)
                            .name(rMsg("点击取消准备", NamedTextColor.RED))
                            .lore(rMsg("已准备人数：%d".formatted(prepared.size()), NamedTextColor.GRAY))
                            .getItem(), (it, pl) -> {
                        //取消准备
                        prepared.remove(pl);
                        update();
                        close = false;
                    });
                } else
                    m.setSlot(15, ItemCreator.create(Material.OBSIDIAN).name(rMsg("点击准备", NamedTextColor.GREEN)).getItem(), (it, pl) -> {
                        //准备
                        prepared.add(pl);
                        update();
                        close = false;
                    });
                if (player1.isOp()) {
                    if (prepared.size() >= 2) {
                        m.setSlot(26, ItemCreator.create(Material.NETHER_STAR).name(rMsg("开始游戏", NamedTextColor.GOLD)).getItem(), (it, pl) -> {
                            List<Player> l = new ArrayList<>(prepared);
                            Collections.shuffle(l);
                            if (game != null) game.destroy();
                            game = new Game(l);
                        });
                    } else m.setSlot(
                            26,
                            ItemCreator.create(Material.BARRIER).name(rMsg("人数不足", NamedTextColor.DARK_RED)).getItem(),
                            (it, pl) -> close = false
                    );
                }
            }
        });
    }
}
