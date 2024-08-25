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
        return remained_tick <= 0 || !run();
    }

    abstract protected boolean run();

}

