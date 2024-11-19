package mc.alive.tick;

import mc.alive.Game;
import mc.alive.PlayerData;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Marker;

import static mc.alive.Game.game;

public class TriggerableTickrunnable implements TickRunnable {
    @SuppressWarnings("DataFlowIssue")
    @Override
    public void tick() {
        if (!Game.isRunning()) return;

        var hunter = game.hunter;
        var pd_hunter = PlayerData.of(hunter);

        //竖直管道
        hunter.getAttribute(Attribute.JUMP_STRENGTH).setBaseValue(.0);
        hunter.getLocation().getNearbyEntitiesByType(Marker.class, 1).forEach(marker -> {
            var tags = marker.getScoreboardTags();
            if (tags.contains("vertical_duct")) {
                hunter.getAttribute(Attribute.JUMP_STRENGTH).setBaseValue(100);
            }
        });
    }
}
