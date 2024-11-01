package mc.alive.role.hunter;

import mc.alive.PlayerData;
import mc.alive.effect.Exposure;
import mc.alive.effect.Giddy;
import mc.alive.effect.Slowness;
import mc.alive.role.Skill;
import mc.alive.util.ItemBuilder;
import mc.alive.util.LocationFactory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

import static mc.alive.Alive.plugin;
import static mc.alive.util.Message.rMsg;

public class SmokeGhost extends Hunter {
    private boolean ghost = false;

    public SmokeGhost(Player pl) {
        super(pl, 100);
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
        return List.of(
                ItemBuilder.material(Material.FIREWORK_STAR)
                        .name(rMsg("迷雾"))
                        .lore(Collections.singletonList(rMsg("")))
                        .build(),
                ItemBuilder.material(Material.FIREWORK_STAR)
                        .name(rMsg("烟界"))
                        .lore(Collections.singletonList(rMsg("")))
                        .build()
        );
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
        player.getInventory().setItem(0, ItemBuilder.material(Material.DIAMOND_HOE, 10100).name(rMsg("<gold><bold>test")).build());
    }

    @Skill(name = "雾障", id = 1)
    public void smoke() {
        Location location = player.getLocation();
        addSkillLocation(location, new Runnable() {
            int time = 0;

            @Override
            public void run() {
                player.getWorld().spawnParticle(Particle.SMOKE, location, 10, 0.5, 0, 0.5, 0.1);
                if (skillFeature == 0) {
                    location.getNearbyPlayers(2, 0.01, pl -> !pl.equals(player))
                            .forEach(pl -> {
                                PlayerData playerData = getPlayerData(pl);
                                playerData.addEffect(new Giddy(pl, 40));
                                playerData.addEffect(new Exposure(pl, 300));
                            });
                    removeSkillLocation(location);
                } else {
                    location.getNearbyPlayers(2, 0.01, pl -> !pl.equals(player))
                            .forEach(pl -> getPlayerData(pl).addEffect(new Slowness(pl, 20, 1)));
                    if (time++ == 100) removeSkillLocation(location);
                }
            }
        }, 1);
        setSKillCD(1, 200);
    }

    @Skill(name = "鬼烟", id = 2)
    public void ghostSmoke() {
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 100, 0.5, 0, 0.5, 0.1);
        Location loc = new Location(player.getWorld(), 0, -1, 0);
        if (ghost) {
            //退出二重世界
            removeSkillLocation(loc);
            getGame().survivors.forEach(pl -> {
                pl.showPlayer(plugin, player);
                player.showPlayer(plugin, pl);
            });
        } else {
            //进入二重世界
            addSkillLocation(loc, () -> {
                if (skillFeature == 0) {
                    player.getWorld().spawnParticle(Particle.ASH, player.getLocation(), 1);
                    getGame().survivors.forEach(pl -> {
                        var v = pl.getLocation().subtract(player.getLocation()).toVector().normalize().multiply(5);
                        for (Location location : LocationFactory.line(player.getEyeLocation(), player.getEyeLocation().add(v), 0.5)) {
                            player.spawnParticle(Particle.DUST, location, 1, 0, 0, 0, 0.1, new Particle.DustOptions(org.bukkit.Color.fromRGB(0, 0, 255), 1));
                        }
                    });
                } else getGame().survivors.forEach(pl ->
                        player.spawnParticle(Particle.DUST, pl.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0.1, new Particle.DustOptions(org.bukkit.Color.fromRGB(0, 0, 255), 1))
                );
            }, 10);
            getGame().survivors.forEach(pl -> {
                pl.hidePlayer(plugin, player);
                player.hidePlayer(plugin, pl);
            });
        }

        ghost = !ghost;
        setSKillCD(2, 100);
    }
}
