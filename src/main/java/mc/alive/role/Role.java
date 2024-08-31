package mc.alive.role;

import mc.alive.Alive;
import mc.alive.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static mc.alive.Alive.plugin;

public abstract class Role {
    /**
     * 与技能相关的locations
     */
    public final Map<Location, BukkitTask> skill_locations = new HashMap<>();
    protected final Player player;
    protected int level = 0;

    public Role(Player p) {
        player = p;
    }

    public static Role of(int id, Player player) {
        var name = (String) Alive.roles_config.get(String.valueOf(id));
        if (name == null) return null;
        Class<?> clazz = null;
        String s = name.split(" ")[0];
        try {
            clazz = Class.forName("mc.alive.role.survivor." + s);
        } catch (ClassNotFoundException _) {
            try {
                clazz = Class.forName("mc.alive.role.hunter." + s);
            } catch (ClassNotFoundException ex) {
                plugin.getLogger().warning(ex.getLocalizedMessage());
            }
        }
        try {
            return (Role) (clazz != null ? clazz.getDeclaredConstructor(Player.class).newInstance(player) : null);
        } catch (Exception e) {
            plugin.getLogger().warning(e.getLocalizedMessage());
        }
        return null;
    }

    public void removeSkillLocation(final Location location) {
        BukkitTask bukkitTask = skill_locations.get(location);
        if (bukkitTask != null && !bukkitTask.isCancelled()) bukkitTask.cancel();
        skill_locations.remove(location);
    }

    public abstract int getRoleID();

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

    protected PlayerData getPlayerData() {
        return PlayerData.of(player);
    }
}
