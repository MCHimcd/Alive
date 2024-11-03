package mc.alive.role.survivor;

import mc.alive.StoredData;
import mc.alive.role.Role;
import org.bukkit.entity.Player;


abstract public class Survivor extends Role {
    protected final int feature = StoredData.playerStoredData.get(player).getOption(StoredData.Option.FEATURE);
    protected boolean hurt = false;
    protected boolean down = false;
    protected boolean captured = false;

    protected Survivor(Player p, int id) {
        super(p, id);
    }


    public int getFeature() {
        return feature;
    }

    /**
     * @return 维修速度
     */
    abstract public int getFixSpeed();

    public boolean isHurt() {
        return hurt;
    }

    public void setHurt(boolean hurt) {
        this.hurt = hurt;
    }

    public boolean isDown() {
        return down;
    }

    public void setDown(boolean down) {
        this.down = down;
    }

    public boolean isCaptured() {
        return captured;
    }

    public void setCaptured(boolean captured) {
        this.captured = captured;
    }
}
