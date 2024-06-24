package mc.alive.role.Butchers;

import mc.alive.role.Sailors.New;
import mc.alive.role.Sailors.Sailor;
import org.bukkit.entity.Player;

abstract public class Butcher {
    protected final Player p;
    public Butcher(Player pl) {
        p = pl;
    }
    //技能 &&被动
    abstract public void skill1();
    abstract public void skill2();
    abstract public void skill3();
    abstract public void skill4();
    abstract public void skill5();
    abstract public void skill6();
    abstract public void passive();
    //初始物品给予
    abstract public void equip();
    //力量
    abstract public double getStrength();
    //护甲
    abstract public int getArmor();
    //速度
    abstract public double getSpeed();
    //最大等级
    abstract public int getMaxLevel();
    //最大血量
    abstract public double getMaxHealth();
    //名字
    abstract public String getName();
    public static Butcher getButcher(int id, Player p) {
        return switch (id) {
            case 1 -> new Hunter(p); //狩猎者
            default -> new Hunter(p);
        };
    }
    public Player getPlayer() {
        return p;
    }
}
