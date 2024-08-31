package mc.alive.effect;

import mc.alive.tick.TickRunnable;
import org.bukkit.entity.Player;

public abstract class Effect implements TickRunnable {
    protected Player player;
    protected int remained_tick;

    public Effect(Player player, int tick) {
        this.player = player;
        this.remained_tick = tick;
        startTick();
    }

    public int getTime() {
        return remained_tick;
    }

    public void addTime(int tick) {
        this.remained_tick += tick;
    }

    public boolean shouldRemove() {
        return !run() || remained_tick <= 0;
    }

    /**
     * @return false则清除此效果
     */
    abstract protected boolean run();

    @Override
    public void tick() {
        remained_tick--;
    }

}

