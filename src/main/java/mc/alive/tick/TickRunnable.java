package mc.alive.tick;

public interface TickRunnable {
    void tick();

    default void startTick() {
        TickRunner.tickRunnable.add(this);
    }
    
}
