package mc.alive.tick;

import mc.alive.Game;
import mc.alive.PlayerData;
import mc.alive.role.hunter.Alien;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
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
        hunter.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).setBaseValue(.42);
        hunter.getLocation().getNearbyEntitiesByType(Marker.class, 1).forEach(marker -> {
            var tags = marker.getScoreboardTags();
            if (tags.contains("vertical_duct")) {
                hunter.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).setBaseValue(100);
            }
        });

        if (pd_hunter.getRole() instanceof Alien alien) {
            alien.setChoosingEffect(false, null);
            var result = hunter.getWorld().rayTraceEntities(hunter.getLocation().add(0, -1, 0), hunter.getLocation().getDirection(), 10, 1);
            if (result != null) {
                var entity = result.getHitEntity();
                if (alien.skill_locations.containsKey(entity.getLocation()) && entity instanceof ArmorStand && entity.getScoreboardTags().contains("body")) {
                    alien.setChoosingEffect(true, entity.getLocation());
                }
            }
        }
    }
}
