package mc.alive.role.hunter;

import mc.alive.Game;
import mc.alive.PlayerData;
import mc.alive.role.Role;
import mc.alive.role.survivor.Survivor;
import mc.alive.tick.TickRunnable;
import mc.alive.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedList;
import java.util.List;

import static mc.alive.Alive.plugin;
import static mc.alive.util.Message.rMsg;

abstract public class Hunter extends Role implements TickRunnable {
    protected List<Player> captured = new LinkedList<>();
    protected int skillFeature = -1;
    protected int otherFeature = -1;
    protected int pursuitFeature = -1;
    protected int level = 0;

    protected Hunter(Player p, int id) {
        super(p, id);
        startTick();
    }

    /**
     * @return 显示在选择菜单的绿色特质（其他类）
     */
    public static List<ItemStack> getOtherFeatures() {
        return List.of(
                ItemBuilder.material(Material.IRON_AXE)
                        .name(rMsg("破坏"))
                        .build()
        );
    }

    /**
     * @return 显示在选择菜单的蓝色特质（追击类）
     */
    public static List<ItemStack> getPursuitFeatures() {
        return List.of(
                ItemBuilder.material(Material.RED_DYE)
                        .name(rMsg("心跳"))
                        .build()
        );
    }

    public int getLevel() {
        return level;
    }

    public int getOtherFeature() {
        return otherFeature;
    }

    /**
     * @param id 特质的id
     */
    public void setOtherFeature(int id) {
        otherFeature = id;
    }

    public int getPursuitFeature() {
        return pursuitFeature;
    }

    /**
     * @param id 特质的id
     */
    public void setPursuitFeature(int id) {
        pursuitFeature = id;
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
     * @return 攻击间隔(秒)
     */
    abstract public double getAttackCD();

    /**
     * @return 最大生命
     */
    abstract public double getMaxHealth();

    /**
     * @param id 特质的id
     */
    public void setSkillFeature(int id) {
        skillFeature = id;
    }

    /**
     * @return 显示在选择菜单的红色特质（改变技能）,从0开始
     */
    abstract public List<ItemStack> getRedFeatures();

    public void addCaptured(Player pl) {
        captured.add(pl);
        player.hidePlayer(plugin, pl);
        ((Survivor) PlayerData.of(pl).getRole()).setCaptured(true);
    }

    public void removeCaptured(Player pl) {
        captured.remove(pl);
        player.showPlayer(plugin, pl);
        ((Survivor) PlayerData.of(pl).getRole()).setCaptured(false);
    }

    @Override
    public void tick() {
        if (!Game.isRunning()) return;
        captured.forEach(pl -> {
            pl.teleport(player);
            pl.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 100, true, false));
        });
    }
}
