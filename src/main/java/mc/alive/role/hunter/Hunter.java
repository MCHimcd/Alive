package mc.alive.role.hunter;

import mc.alive.Game;
import mc.alive.PlayerData;
import mc.alive.mechanism.GhostDom;
import mc.alive.role.Role;
import mc.alive.role.survivor.Survivor;
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

abstract public class Hunter extends Role {
    protected List<Player> captured = new LinkedList<>();
    protected int skillFeature = -1;
    protected int otherFeature = -1;
    protected int pursuitFeature = -1;
    protected int level = 0;
    protected boolean strengthened = false; //第二阶段强化
    private int break_tick = 0;    //hunter破坏机子cd
    private double attack_cd = -1;  //攻击cd
    private int health_tick = 0;    //回血间隔

    protected Hunter(Player p, int id) {
        super(p, id);
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

    public void resetHealthTick(double health) {
        health_tick = (int) Math.max(100, (Math.max(0, health / getMaxHealth()) * 400));
    }

    /**
     * @return 最大生命
     */
    abstract public double getMaxHealth();

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
     * 设置攻击cd
     */
    public void attack() {
        attack_cd = getAttackCD();
    }

    /**
     * @return 攻击间隔(秒)
     */
    abstract public double getAttackCD();

    /**
     * 第二阶段的强化
     */
    public void strengthen() {
        strengthened = true;
    }

    /**
     * @return 是否可以攻击
     */
    public boolean canAttack() {
        return attack_cd == 0;
    }

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

    public void sealCaptured(GhostDom dom) {
        captured.forEach(pl -> {
            getGame().sealSurvivor(pl, dom);
            removeCaptured(pl);
        });
    }

    public void removeCaptured(Player pl) {
        captured.remove(pl);
        player.showPlayer(plugin, pl);
        ((Survivor) PlayerData.of(pl).getRole()).setCaptured(false);
    }

    public boolean breakTick() {
        if (break_tick-- == 0) break_tick = 90 * 20;
        return break_tick <= 0;
    }


    @Override
    public void tick() {
        if (!Game.isRunning()) return;
        //抓捕
        captured.forEach(pl -> {
            pl.teleport(player);
            pl.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 100, true, false));
        });
        //普攻冷却
        attack_cd = Math.max(0, attack_cd - 0.05);
        if (attack_cd > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 1, 100, true, false));
        }
        //血量恢复
        if (health_tick > 0) health_tick--;
        if (health_tick == 0) {
            PlayerData.of(player).damageOrHeal(-0.25);
        }
        //心跳
        player.getWorld().getNearbyPlayers(player.getLocation(), pursuitFeature == 0 ? 12 : 18, p -> !p.equals(player))
                .forEach(pl -> ((Survivor) PlayerData.of(pl).getRole()).heartbeatTick());
    }

}
