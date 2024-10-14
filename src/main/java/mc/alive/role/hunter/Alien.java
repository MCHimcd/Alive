package mc.alive.role.hunter;

import mc.alive.effect.Invisibility;
import mc.alive.role.Skill;
import mc.alive.util.ItemBuilder;
import mc.alive.util.LocationFactory;
import mc.alive.util.Message;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

import static mc.alive.Alive.plugin;

public class Alien extends Hunter {
    private boolean choosing_effect = false;
    private Location choose_location;

    public Alien(Player pl) {
        super(pl);
    }

    @Override
    public int getRoleID() {
        return 100;
    }

    @Override
    public int getStrength() {
        return 20;
    }

    @Override
    public double getSpeed() {
        return 0.1;
    }

    @Override
    public double getMaxHealth() {
        return 50;
    }

    @Override
    public void equip() {
        player.getInventory().setItem(0, ItemBuilder.material(Material.DIAMOND_HOE, 10100).name(Message.rMsg("<red><bold>手镰")).build());
    }

    @Override
    public double getAttackRange() {
        return 3;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public double getAttackCD() {
        return 1.8;
    }

    @Override
    public List<ItemStack> getRedFeatures() {
        return List.of(new ItemStack(Material.DIAMOND));
    }

    @Override
    public List<ItemStack> getGreenFeatures() {
        return List.of(new ItemStack(Material.DIAMOND));
    }

    @Override
    public List<ItemStack> getBlueFeatures() {
        return List.of(new ItemStack(Material.DIAMOND));
    }

    /**
     * 捡尸体回血
     */
    @Skill(id = 1, name = "噬尽")
    public void health() {
        if (choosing_effect) {
            player.playSound(player, Sound.ENTITY_WANDERING_TRADER_DRINK_POTION, 1f, 1f);
            Location loc = choose_location.clone();
            new BukkitRunnable() {
                int t = 0;

                @Override
                public void run() {
                    if (t++ >= 40) {
                        cancel();
                    } else {
                        getPlayerData().damageOrHeal(-2);
                        List<Location> line = LocationFactory.line(player.getLocation(), loc.clone().add(0, 1, 0), 1);
                        line.forEach(location -> player.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION,
                                location,
                                1, 0, 0, 0, 0,
                                new Particle.DustTransition(Color.RED, Color.PURPLE, 1f)));
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
            removeSkillLocation(choose_location);
        } else {
            player.sendMessage(Message.rMsg("- <color:#7a00e6>附近无可用灵魂</color>"));
        }
    }

    /**
     * 捡尸体加速隐身
     */
    @Skill(id = 2, name = "灭绝", minLevel = 1)
    public void speed() {
        if (choosing_effect) {
            player.playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1, false, false));
            getPlayerData().addEffect(new Invisibility(player, 100));
            removeSkillLocation(choose_location);
        } else {
            player.sendMessage(Message.rMsg("- <color:#7a00e6>附近无可用灵魂</color>"));
        }
    }

    /**
     * @param choosing_effect 准星是否对着可作为技能选择的尸体
     * @param target          目标位置
     */
    public void setChoosingEffect(boolean choosing_effect, Location target) {
        this.choosing_effect = choosing_effect;
        choose_location = target;
    }
}
