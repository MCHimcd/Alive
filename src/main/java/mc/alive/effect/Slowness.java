package mc.alive.effect;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Slowness extends MultilevelEffect {
    public Slowness(Player player, int tick, int level) {
        super(player, tick, level);
    }

    @Override
    protected boolean run() {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1, level, false, false));
        return true;
    }
}
