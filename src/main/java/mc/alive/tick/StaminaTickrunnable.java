package mc.alive.tick;

import mc.alive.Game;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

public class StaminaTickrunnable implements TickRunnable {
    @SuppressWarnings("DataFlowIssue")
    @Override
    public void tick() {
        if (!Game.isStarted()) return;
        Game.game.playerData.forEach((player, pd) -> {
            if (player.isSprinting()) pd.addStamina(-1);
            AttributeInstance jump = player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH);
            if (pd.addStamina(0)) {
                player.setFoodLevel(20);
                jump.setBaseValue(.42);
            } else {
                player.setFoodLevel(6);
                jump.setBaseValue(0);
            }
        });
    }
}
