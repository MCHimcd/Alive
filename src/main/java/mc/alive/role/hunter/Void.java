package mc.alive.role.hunter;

import mc.alive.Alive;
import mc.alive.PlayerData;
import mc.alive.role.Skill;
import mc.alive.util.ItemBuilder;
import mc.alive.util.Message;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class Void extends Hunter {
    private int skill1_time = -1;

    public Void(Player pl) {
        super(pl);
    }

    @Override
    public double getAttackRange() {
        return 2;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public double getAttackCD() {
        return 2;
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

    @Override
    public int getRoleID() {
        return 101;
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
        player.getInventory().setItem(0, ItemBuilder.material(Material.DIAMOND_HOE, 10101).name(Message.rMsg("<red><bold>爪")).build());
    }

    @Skill(id = 1, name = "轮转")
    public void teleport() {
        var skill1_task = skill_locations.get(ZERO_LOC);
//        if (skill1_task == null) skill1_time = -1;
        if (skill1_time == -1) {
            skill_locations.put(ZERO_LOC,
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.sendMessage(Message.rMsg("%s".formatted(skill1_time)));
                            if (skill1_time < 60) {
                                player.spawnParticle(Particle.WITCH, player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize().multiply(0.3)), 0, 0, 0, 0, 0);
                                skill1_time++;
                                player.sendActionBar(Message.rMsg("<aqua>■".repeat(skill1_time / 5) + "<aqua>□".repeat(12 - skill1_time / 5)));
                            }
                        }
                    }.runTaskTimer(Alive.plugin, 0, 1));
        } else {
            if (!skill1_task.isCancelled()) skill1_task.cancel();
            player.teleport(player.getLocation().add(player.getEyeLocation().getDirection().normalize().multiply(skill1_time / 4)));
            skill1_time = 0;
            PlayerData.setSkillCD(player, 1, 100);
        }
    }
}
