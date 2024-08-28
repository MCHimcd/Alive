package mc.alive.tick;

import mc.alive.Game;

public class StaminaTickrunnable implements TickRunnable {
    @Override
    public void tick() {
        if (!Game.isStarted()) return;
        Game.game.playerData.forEach((player, pd) -> {
            if (player.isSprinting()) pd.addStamina(-1);
            if (pd.addStamina(0)) {
                player.setFoodLevel(20);
            } else {
                player.setFoodLevel(6);
            }
        });
    }
}
