package mc.alive.effect;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Speed extends MultilevelEffect {
    public Speed(Player player, int tick, int level) {
        super(player, tick, level);
    }

    @Override
    protected boolean run() {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1, level, false, false));
        return true;
    }
}
