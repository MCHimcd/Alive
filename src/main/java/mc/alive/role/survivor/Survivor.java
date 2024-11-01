package mc.alive.role.survivor;

import mc.alive.StoredData;
import mc.alive.role.Role;
import org.bukkit.entity.Player;


abstract public class Survivor extends Role {
    protected final int feature = StoredData.playerStoredData.get(player).getOption(StoredData.Option.FEATURE);

    protected Survivor(Player p, int id) {
        super(p, id);
    }


    public int getFeature() {
        return feature;
    }

    /**
     * @return 最大护盾
     */
    abstract public int getMaxShield();

    /**
     * @return 维修速度
     */
    abstract public int getFixSpeed();

}
