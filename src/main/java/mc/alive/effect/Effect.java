package mc.alive.effect;

import mc.alive.tick.TickRunnable;
import mc.alive.tick.TickRunner;
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
        boolean b = !run() || remained_tick <= 0;
        if (b) TickRunner.tickRunnable.remove(this);
        return b;
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

