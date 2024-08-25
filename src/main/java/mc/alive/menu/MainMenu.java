package mc.alive.menu;

import mc.alive.Game;
import mc.alive.util.ItemBuilder;
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

import static mc.alive.Game.game;
import static mc.alive.util.Message.convertMsg;
import static mc.alive.util.Message.rMsg;

public class MainMenu extends SlotMenu {
    public static final List<Player> prepared_players = new ArrayList<>();
    public static final List<Player> players_looking_document = new ArrayList<>();

    public MainMenu(Player p) {
        super(27, Component.text("主菜单", NamedTextColor.GOLD), p);

        // 资料
        setSlot(11, ItemBuilder.material(Material.BOOK)
                .name(rMsg("<aqua>资料"))
                .build(), (it, pl) -> {
            pl.setGameMode(GameMode.SPECTATOR);
            players_looking_document.add(pl);
            pl.closeInventory();
            pl.openBook(Book.builder().pages(convertMsg(List.of(
                    "<blue>document"
            ))).build());
            pl.teleport(pl);
            close = false;
        });

        // 加入游戏
        if (prepared_players.contains(p)) {
            setSlot(15, ItemBuilder.material(Material.CRYING_OBSIDIAN)
                    .name(rMsg("<red>点击取消准备"))
                    .lore(Collections.singletonList(rMsg("<gray>已准备人数：%d".formatted(prepared_players.size()))))
                    .build(), (it, pl) -> {
                // 取消准备
                prepared_players.remove(pl);
                update();
                close = false;
            });
        } else {
            setSlot(15, ItemBuilder.material(Material.OBSIDIAN)
                    .name(rMsg("<green>点击准备"))
                    .build(), (it, pl) -> {
                // 准备
                prepared_players.add(pl);
                update();
                close = false;
            });
        }

        // 管理员选项
        if (p.isOp()) {
            if (prepared_players.size() >= 2) {
                setSlot(26, ItemBuilder.material(Material.NETHER_STAR)
                        .name(rMsg("<gold>开始游戏"))
                        .build(), (it, pl) -> {
                    List<Player> l = new ArrayList<>(prepared_players);
                    Collections.shuffle(l);
                    if (game != null) game.destroy();
                    game = new Game(l);
                });
            } else {
                setSlot(26, ItemBuilder.material(Material.BARRIER)
                        .name(rMsg("<dark_red>人数不足"))
                        .build(), (it, pl) -> close = false);
            }
        }
    }

    private void update() {
        Bukkit.getOnlinePlayers().forEach(player1 -> {
            if (player1.getOpenInventory().getTopInventory().getHolder() instanceof MainMenu m) {
                if (prepared_players.contains(player1)) {
                    m.setSlot(15, ItemBuilder.material(Material.CRYING_OBSIDIAN)
                            .name(rMsg("<red>点击取消准备"))
                            .lore(Collections.singletonList(rMsg("<gray>已准备人数：%d".formatted(prepared_players.size()))))
                            .build(), (it, pl) -> {
                        // 取消准备
                        prepared_players.remove(pl);
                        update();
                        close = false;
                    });
                } else {
                    m.setSlot(15, ItemBuilder.material(Material.OBSIDIAN)
                            .name(rMsg("<green>点击准备"))
                            .build(), (it, pl) -> {
                        // 准备
                        prepared_players.add(pl);
                        update();
                        close = false;
                    });
                }

                if (player1.isOp()) {
                    if (prepared_players.size() >= 2) {
                        m.setSlot(26, ItemBuilder.material(Material.NETHER_STAR)
                                .name(rMsg("<gold>开始游戏"))
                                .build(), (it, pl) -> {
                            List<Player> l = new ArrayList<>(prepared_players);
                            Collections.shuffle(l);
                            if (game != null) game.destroy();
                            game = new Game(l);
                        });
                    } else {
                        m.setSlot(26, ItemBuilder.material(Material.BARRIER)
                                .name(rMsg("<dark_red>人数不足"))
                                .build(), (it, pl) -> close = false);
                    }
                }
            }
        });
    }
}
