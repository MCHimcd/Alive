package mc.alive.mechanism;

import mc.alive.Game;
import mc.alive.PlayerData;
import mc.alive.effect.Giddy;
import mc.alive.tick.TickRunnable;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;

import static mc.alive.Alive.plugin;
import static mc.alive.Game.game;
import static mc.alive.util.Message.rMsg;

public class Barrier implements TickRunnable {
    public static final Map<Player, Barrier> chosenBarriers = new HashMap<>();
    private final BlockFace face;
    private final Location start;
    private final BoundingBox boundingBox;
    public boolean isChosen = false;
    public int tick = 0;
    public BukkitTask tick_task = null;
    private boolean triggered = false;

    public Barrier(Location start, BlockFace face) {
        this.start = start;
        this.face = face;
        boundingBox = new BoundingBox(
                start.getX(), start.getY(), start.getZ(),
                start.getX() + (face == BlockFace.EAST ? 2 : 1), start.getY() + 2, start.getZ() + (face == BlockFace.NORTH ? 2 : 1)
        );
        startTick();
    }

    /**
     * @return 是否已放倒
     */
    public boolean isTriggered() {
        return triggered;
    }

    public BlockFace getFace() {
        return face;
    }

    /**
     * 尝试触发改变
     * @param destroy 破坏或放倒
     * @param player 触发的玩家
     */
    public void trigger(boolean destroy, Player player) {
        if (tick_task != null || (destroy && !triggered) || (!destroy && triggered)) return;
        tick_task = new BukkitRunnable() {
            @Override
            public void run() {
                player.sendActionBar(rMsg(String.valueOf(tick)));
                if (++tick >= 30) {
                    if (destroy) {
                        game.barriers.remove(start);
                        //todo 游戏中显示
                    } else {
                        Player hunter = game.hunter;
                        if (boundingBox.overlaps(hunter.getBoundingBox())) {
                            PlayerData.of(hunter).addEffect(new Giddy(hunter, 20));
                        }
                        triggered = true;
                        tick = 0;
                        //todo 游戏中显示
                    }
                    cancel();
                    tick_task = null;
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    @Override
    public void tick() {
        if (!Game.isRunning()) return;
        isChosen = false;
        start.getNearbyPlayers(4).forEach(player -> {
            BoundingBox boundingBox = getBoundingBox();
            RayTraceResult result = boundingBox.rayTrace(player.getEyeLocation().toVector(), player.getEyeLocation().getDirection(), 3);
            if (result != null) {
                isChosen = true;
                chosenBarriers.put(player, this);
            }
        });
        if (!isChosen && tick_task != null) {
            tick = 0;
            tick_task.cancel();
            tick_task = null;
        }
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }
}
