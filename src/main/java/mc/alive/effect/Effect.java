package mc.alive.effect;

import org.bukkit.entity.Player;

public abstract class Effect {
    protected Player player;
    protected int remained_tick;

    public Effect(Player player, int tick) {
        this.player = player;
        this.remained_tick = tick;
    }

    public int getTime() {
        return remained_tick;
    }

    public void addTime(int tick) {
        this.remained_tick += tick;
    }

    public boolean tick() {
        remained_tick--;
        return !run() || remained_tick <= 0;
    }

    /**
     * @return false则清除此效果
     */
    abstract protected boolean run();

}

