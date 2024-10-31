package mc.alive.role.hunter;

import mc.alive.effect.Slowness;
import mc.alive.role.Skill;
import mc.alive.util.ItemBuilder;
import mc.alive.util.Message;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static mc.alive.Alive.plugin;

public class SmokeGhost extends Hunter {
    private boolean ghost = false;

    public SmokeGhost(Player pl) {
        super(pl);
    }

    @Override
    public double getAttackRange() {
        return 2;
    }

    @Override
    public int getMaxLevel() {
        return 0;
    }

    @Override
    public double getAttackCD() {
        return 40;
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
        return 100;
    }

    @Override
    public int getStrength() {
        return 30;
    }

    @Override
    public double getSpeed() {
        return 0.2;
    }

    @Override
    public double getMaxHealth() {
        return 80;
    }

    @Override
    public void equip() {
        player.getInventory().setItem(0, ItemBuilder.material(Material.DIAMOND_HOE, 10100).name(Message.rMsg("<gold><bold>test")).build());
    }

    @Skill(name = "雾障", id = 1)
    public void smoke() {
        Location location = player.getLocation();
        addSkillLocation(location, new Runnable() {
            int time = 0;

            @Override
            public void run() {
                player.getWorld().spawnParticle(Particle.SMOKE, location, 10, 0.5, 0, 0.5, 0.1);
                location.getNearbyPlayers(2, 0.01, pl -> !pl.equals(player))
                        .forEach(pl -> getPlayerData(pl).addEffect(new Slowness(pl, 20, 1)));
                if (time++ == 100) removeSkillLocation(location);
            }
        }, 1);
        setSKillCD(1, 100);
    }

    @Skill(name = "鬼烟", id = 2)
    public void ghostSmoke() {
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 100, 0.5, 0, 0.5, 0.1);
        Location loc = new Location(player.getWorld(), 0, -1, 0);
        if (ghost) {
            removeSkillLocation(loc);
            getGame().survivors.forEach(pl -> {
                pl.showPlayer(plugin, player);
                player.showPlayer(plugin, pl);
            });
        } else {
            addSkillLocation(loc, () ->
                    getGame().survivors.forEach(pl ->
                            player.spawnParticle(Particle.DUST, pl.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0.1, new Particle.DustOptions(org.bukkit.Color.fromRGB(0, 0, 255), 1))
                    ), 10);
            getGame().survivors.forEach(pl -> {
                pl.hidePlayer(plugin, player);
                player.hidePlayer(plugin, pl);
            });
        }
        ghost = !ghost;
        setSKillCD(2, 100);
    }
}
