package mc.alive.role;

import mc.alive.role.hunter.HunterTodo;
import mc.alive.role.survivor.SurvivorTodo;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Map;

public abstract class Role {
    public final static Map<Integer, String> names = Map.of(
            100, "§c狩猎者todo",
            200, "§a杰克",
            201, "§a杰克",
            202, "§a杰克"
    );
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

    //攻击间隔
    abstract public double getAttackCD();

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
            case 200, 201, 202 -> new SurvivorTodo(player);
            default -> null;
        };
    }
}
