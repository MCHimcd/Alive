package mc.alive.role;

import mc.alive.Alive;
import mc.alive.Game;
import mc.alive.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Range;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static mc.alive.Alive.plugin;

public abstract class Role {
    /**
     * 用于蓄力技能
     */
    public static final Location ZERO_LOC = new Location(Bukkit.getWorld("world"), 0, 0, 0);
    /**
     * 与技能相关的locations
     */
    private final Map<Location, BukkitTask> skill_locations = new HashMap<>();
    protected Player player;
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

    public Set<Location> getSkillLocations() {
        return skill_locations.keySet();
    }

    public void removeSkillLocation(Location location) {
        BukkitTask bukkitTask = skill_locations.get(location);
        if (bukkitTask != null && !bukkitTask.isCancelled()) bukkitTask.cancel();
        skill_locations.remove(location);
    }

    public void addSkillLocation(Location location, Runnable task, long period) {
        skill_locations.put(location, Bukkit.getScheduler().runTaskTimer(plugin, task, 0, period));
    }

    /**
     * @return 角色id，用于从配置文件中获取角色名
     */
    public abstract @Range(from = 100, to = 300) int getRoleID();

    public int getLevel() {
        return level;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    /**
     * @return 力量
     */
    abstract public int getStrength();

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
        return getPlayerData(player);
    }

    protected PlayerData getPlayerData(Player player) {
        return PlayerData.of(player);
    }

    protected Game getGame() {
        return Game.game;
    }

    protected void setSKillCD(int id, int cd) {
        PlayerData.setSkillCD(player, id, cd);
    }
}
