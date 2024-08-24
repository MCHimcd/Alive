package mc.alive.game.role;

import mc.alive.game.role.hunter.Alien;
import mc.alive.game.role.survivor.Dealt;
import mc.alive.game.role.survivor.Jack;
import mc.alive.game.role.survivor.Mike;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class Role {
    public final static Map<Integer, String> names = Map.of(
            100, "§c异形",
            200, "§a杰克",
            201, "§a迈克",
            202, "§a德尔塔"
    );
    //技能location
    public final Map<Location, BukkitTask> skill_locations = new HashMap<>();
    protected final Player player;
    protected int level = 0;

    public Role(Player p) {
        player = p;
    }

    public static Role of(int id, Player player) {
        return switch (id) {
            case 100 -> new Alien(player);
            case 200 -> new Jack(player);
            case 201 -> new Mike(player);
            case 202 -> new Dealt(player);
            default -> null;
        };
    }

    public int getLevel() {
        return level;
    }

    public Player getPlayer() {
        return player;
    }

    /**
     * @return 力量
     */
    abstract public int getStrength();

    /**
     * @return 攻击间隔
     */
    abstract public double getAttackCD();

    /**
     * @return 移动速度
     */
    abstract public double getSpeed();

    /**
     * @return 最大生命
     */
    abstract public double getMaxHealth();


    /**
     * 初始物品给予
     */
    abstract public void equip();

    /**
     * @return 技能数量
     */
    public int getSkillCount() {
        return Math.max(2, (int) Arrays.stream(getClass().getMethods())
                .filter(m -> m.isAnnotationPresent(Skill.class))
                .count());
    }
}
