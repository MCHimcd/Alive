package mc.alive.role.hunter;

import mc.alive.role.Role;
import org.bukkit.entity.Player;

import static mc.alive.util.Message.rMsg;

abstract public class Hunter extends Role {
    public Hunter(Player pl) {
        super(pl);
    }

    abstract public double getAttackRange();

    public void levelUp() {
        player.sendMessage(rMsg("你升级了"));
        level = Math.max(getMaxLevel(), level + 1);
    }

    /**
     * @return 最大等级
     */
    abstract public int getMaxLevel();

    /**
     * @return 攻击间隔
     */
    abstract public double getAttackCD();

}
