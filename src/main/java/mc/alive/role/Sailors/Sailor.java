package mc.alive.role.Sailors;

import org.bukkit.entity.Player;

abstract public class Sailor {
    protected final Player p;
    public Sailor(Player pl) {
        p = pl;
    }
    //技能&&被动
    abstract public void skill1();
    abstract public void skill2();
    abstract public void passive();
    //初始物品给予
    abstract public void equip();
    //力量
    abstract public double getStrength();
    //护甲
    abstract public int getArmor();
    //速度
    abstract public double getSpeed();
    //最大生命
    abstract public double getMaxHealth();
    //名字
    abstract public String getName();
    public static Sailor getSailor(int id, Player p) {
        return switch (id) {
            case 1 -> new New(p);
            default -> new New(p);
        };
    }


    public Player getPlayer() {
        return p;
    }

}
