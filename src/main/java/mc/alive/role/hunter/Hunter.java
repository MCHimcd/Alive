package mc.alive.role.hunter;

import mc.alive.role.Role;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static mc.alive.util.Message.rMsg;

abstract public class Hunter extends Role {
    protected int redFeature = -1;
    protected int greenFeature = -1;
    protected int blueFeature = -1;

    public Hunter(Player pl) {
        super(pl);
    }

    abstract public double getAttackRange();

    public void levelUp() {
        if (level >= getMaxLevel()) {
            player.sendMessage(rMsg("你已经达到最高等级"));
            return;
        }
        player.sendMessage(rMsg("你升级了"));
        level += 1;
    }

    /**
     * @return 最大等级
     */
    abstract public int getMaxLevel();

    /**
     * @return 攻击间隔(tick)
     */
    abstract public double getAttackCD();

    /**
     * @param id 特质的id
     */
    public void setRedFeature(int id) {
        redFeature = id;
    }

    /**
     * @param id 特质的id
     */
    public void setGreenFeature(int id) {
        greenFeature = id;
    }

    /**
     * @param id 特质的id
     */
    public void setBlueFeature(int id) {
        blueFeature = id;
    }

    /**
     * @return 显示在选择菜单的红色特质
     */
    abstract public List<ItemStack> getRedFeatures();

    /**
     * @return 显示在选择菜单的绿色特质
     */
    abstract public List<ItemStack> getGreenFeatures();

    /**
     * @return 显示在选择菜单的蓝色特质
     */
    abstract public List<ItemStack> getBlueFeatures();

}
