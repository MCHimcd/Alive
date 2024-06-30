package mc.alive.role;

import mc.alive.role.hunter.HunterTodo;
import mc.alive.role.survivor.SurvivorTodo;
import org.bukkit.entity.Player;

import java.util.Arrays;

public abstract class Role {
    protected final Player player;
    protected int level = 0;

    public int getLevel() {
        return level;
    }

    public Role(Player p) {
        player = p;
    }

    public Player getPlayer() {
        return player;
    }

    //力量
    abstract public int getStrength();

    //速度
    abstract public double getSpeed();

    //最大生命
    abstract public double getMaxHealth();

    //智力
    abstract public int getIntelligence();

    //初始物品给予
    abstract public void equip();

    public int getSkillCount() {
        return (int) Arrays.stream(getClass().getMethods())
                .filter(m -> m.isAnnotationPresent(Skill.class))
                .count();
    }

    public static Role of(int id, Player player) {
        return switch (id) {
            case 100 -> new HunterTodo(player);
            case 200,201 -> new SurvivorTodo(player);
            default -> null;
        };
    }



/*
人物属性注解:
船员:   基础生命:200  基础护盾:50 基础速度:1  基础力量:10   基础智力:5
10生命 ->  -> 0.1 速度->1 力量-> 1 智力->10 护盾;
所有船员应满足如上转换

屠夫:    基础生命:1000 基础速度:2  基础力量:20  基础智力:1
100生命 -> -> 0.2 速度->1 力量-> 1 智力
所有屠夫应满足如上转换

*/
}
