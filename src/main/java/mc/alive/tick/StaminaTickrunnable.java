package mc.alive.tick;

import mc.alive.game.Game;

public class StaminaTickrunnable implements TickRunnable {
    @Override
    public void tick() {
        if (!Game.isStarted()) return;
        Game.game.playerData.forEach((player, pd) -> {
            if (player.isSprinting()) pd.addStamina(-1);
        });
    }
}
