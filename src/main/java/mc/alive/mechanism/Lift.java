package mc.alive.mechanism;

import io.papermc.paper.entity.TeleportFlag;
import mc.alive.Game;
import mc.alive.tick.TickRunnable;
import mc.alive.util.ItemBuilder;
import mc.alive.util.LocationFactory;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static mc.alive.Alive.plugin;
import static mc.alive.util.Message.rMsg;

public class Lift implements TickRunnable {
    private final BlockDisplay blockDisplay;
    private final int max_floor;
    public List<Player> players = new ArrayList<>();
    public List<LiftDoor> liftDoors = new ArrayList<>();
    private int floor = 1;
    private int target_floor = 0;
    private BukkitTask task;

    public Lift(BlockDisplay blockDisplay, int max_floor) {
        this.blockDisplay = blockDisplay;
        this.max_floor = max_floor;
        LocationFactory.replace2x2Lift(blockDisplay.getLocation(), Material.BARRIER, BlockFace.SELF);
        startTick();
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
            result.add(new Result(is, (_, _) -> changeFloorTo(finalI)));
        }
        return result;
    }

    /**
     * @param floor 目标楼层
     */
    public void changeFloorTo(int floor) {
        if (target_floor == floor || (task != null && !task.isCancelled())) return;
        task = new RunnableTask(players, blockDisplay, this).runTaskTimer(plugin, 0, 1);
        players.forEach(HumanEntity::closeInventory);
        target_floor = floor;
    }

    public int getFloor() {
        return floor;
    }

    /**
     * @return 到达下一层所需的时间(tick)
     */
    public int getTargetTime() {
        AtomicInteger y_f = new AtomicInteger();
        AtomicInteger y_tf = new AtomicInteger();
        Game.game.liftDoors.forEach((block, liftDoor) -> {
            if (liftDoor.getFloor() == floor) y_f.set(block.getY());
            if (liftDoor.getFloor() == floor + getTargetDirection()) y_tf.set(block.getY());
        });
        return Math.abs(y_f.get() - y_tf.get()) * 10;
    }

    /**
     * @return 1向上，0不动，-1向下
     */
    public int getTargetDirection() {
        return Integer.compare(target_floor, floor);
    }

    /**
     * @return 当前楼层是否与目标相符
     */
    public boolean changeFloor() {
        floor += getTargetDirection();
        return floor == target_floor;
    }

    @Override
    public void tick() {
        if (!Game.isRunning()) return;
        players.clear();
        blockDisplay.getWorld().getNearbyPlayers(blockDisplay.getLocation(), 3).forEach(player -> {
            if (player.getBoundingBox().overlaps(new BoundingBox(
                    blockDisplay.getX() + 0.6, blockDisplay.getY() + 0.3, blockDisplay.getZ() + 0.6,
                    blockDisplay.getX() + 1.4, blockDisplay.getY() + 1.3, blockDisplay.getZ() + 1.4
            ))) {
                players.add(player);
            }
        });
    }

    public record Result(ItemStack item, BiConsumer<ItemStack, Player> function) {
    }

    private static class RunnableTask extends BukkitRunnable {

        private final List<Player> players;
        private final BlockDisplay blockDisplay;
        private final Lift lift;
        private int t = 0;
        private int target_time = 0;

        public RunnableTask(List<Player> players, BlockDisplay blockDisplay, Lift lift) {
            this.players = new ArrayList<>(players);
            this.blockDisplay = blockDisplay;
            this.lift = lift;
            LocationFactory.replace2x2Lift(blockDisplay.getLocation(), Material.AIR, BlockFace.SELF);
        }

        public void run() {
            if (t == 0) {
                lift.liftDoors.stream().filter(liftDoor -> liftDoor.getFloor() == lift.getFloor()).findFirst().ifPresent(LiftDoor::closeDoor);
                target_time = lift.getTargetTime();
            }
            if (t++ >= target_time) {
                if (lift.changeFloor()) {
                    //到达
                    LocationFactory.replace2x2Lift(blockDisplay.getLocation(), Material.BARRIER, BlockFace.SELF);
                    lift.liftDoors.stream().filter(liftDoor -> liftDoor.getFloor() == lift.getFloor()).findFirst().ifPresent(LiftDoor::openDoor);
                    players.forEach(player -> {
                        player.teleport(
                                blockDisplay.getLocation().add(1.5, 0.5, 1.5),
                                TeleportFlag.Relative.VELOCITY_ROTATION
                        );
                    });
                    cancel();
                } else {
                    t = 0;
                }
            } else {
                var dy = lift.getTargetDirection() * 0.1;
                players.forEach(player -> {
                    player.teleport(
                            player.getLocation().add(0, dy, 0),
                            TeleportFlag.Relative.VELOCITY_ROTATION
                    );
                });
                blockDisplay.teleport(blockDisplay.getLocation().add(0, dy, 0));
            }
        }
    }
}
