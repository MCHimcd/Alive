package mc.alive.mechanism;

import mc.alive.PlayerData;
import mc.alive.effect.Giddy;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import static mc.alive.Alive.plugin;
import static mc.alive.Game.game;
import static mc.alive.util.Message.rMsg;

public class Barrier {
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
    }

    public boolean isTriggered() {
        return triggered;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public BlockFace getFace() {
        return face;
    }

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
}
