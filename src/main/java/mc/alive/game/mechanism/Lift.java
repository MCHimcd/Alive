package mc.alive.game.mechanism;

import io.papermc.paper.entity.TeleportFlag;
import mc.alive.game.Game;
import mc.alive.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

import static mc.alive.Alive.plugin;
import static mc.alive.util.Message.rMsg;

public class Lift {
    private final BlockDisplay blockDisplay;
    private final int max_floor;
    public List<Player> players = new ArrayList<>();
    private int floor = 1;
    private int target_floor = 0;

    public Lift(BlockDisplay blockDisplay, int max_floor) {
        this.blockDisplay = blockDisplay;
        this.max_floor = max_floor;
        var loc = blockDisplay.getLocation();
        Game.replace2x2(blockDisplay.getLocation(), Material.BARRIER);
    }

    /**
     * @return 用于菜单中的物品
     */
    public List<Result> getItemStacks() {
        var result = new LinkedList<Result>();
        for (int i = 1; i <= max_floor; i++) {
            int finalI = i;
            var is = ItemBuilder.material(Material.PAPER).name(rMsg(String.valueOf(i))).amount(i).build();
            if (finalI == floor) is.editMeta(meta -> {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            });
            result.add(new Result(is, (_, _) -> {
                changeFloorTo(finalI);
            }));
        }
        return result;
    }

    /**
     * @param floor 目标楼层
     */
    public void changeFloorTo(int floor) {
        if (target_floor == floor) return;
        players.forEach(HumanEntity::closeInventory);
        target_floor = floor;
        new RunnableTask(players, blockDisplay, this).runTaskTimer(plugin, 0, 1);
    }

    /**
     * @return 获得当前楼层
     */
    public int getFloor() {
        return floor;
    }

    /**
     * @return 当前楼层是否与目标相符
     */
    public boolean changeFloor() {
        floor += getTargetDirection();
        return floor == target_floor;
    }

    /**
     * @return 1向上，0不动，-1向下
     */
    public int getTargetDirection() {
        return Integer.compare(target_floor, floor);
    }

    public record Result(ItemStack item, BiConsumer<ItemStack, Player> function) {
    }

    private static class RunnableTask extends BukkitRunnable {

        private final List<Player> players;
        private final BlockDisplay blockDisplay;
        private final Lift lift;
        private int t = 0;

        public RunnableTask(List<Player> players, BlockDisplay blockDisplay, Lift lift) {
            this.players = players;
            this.blockDisplay = blockDisplay;
            this.lift = lift;
            Game.replace2x2(blockDisplay.getLocation(), Material.AIR);
        }

        public void run() {
            if (t++ >= 30) {
                if (lift.changeFloor()) {
                    var loc = blockDisplay.getLocation();
                    Game.replace2x2(blockDisplay.getLocation(), Material.BARRIER);
                    players.forEach(player -> player.teleport(
                            player.getLocation().add(0, 0.1, 0),
                            TeleportFlag.Relative.YAW,
                            TeleportFlag.Relative.PITCH
                    ));
                    cancel();
                } else {
                    t = 0;
                }
            } else {
                var dy = lift.getTargetDirection() * 0.1;
                players.forEach(player -> player.teleport(
                        player.getLocation().add(0, dy, 0),
                        TeleportFlag.Relative.YAW,
                        TeleportFlag.Relative.PITCH
                ));
                blockDisplay.teleport(blockDisplay.getLocation().add(0, dy, 0));
            }
        }
    }
}
