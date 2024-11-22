package mc.alive.role.hunter;

import mc.alive.PlayerData;
import mc.alive.effect.Exposure;
import mc.alive.effect.Giddy;
import mc.alive.effect.Slowness;
import mc.alive.role.Skill;
import mc.alive.util.ItemBuilder;
import mc.alive.util.LocationFactory;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static mc.alive.Alive.plugin;
import static mc.alive.util.Message.rMsg;

public class SmokeGhost extends Hunter {
    private final List<Location> smoke_locs = new LinkedList<>();
    private boolean ghost = false;

    public SmokeGhost(Player pl) {
        super(pl, 100);
    }

    @Override
    public double getMaxHealth() {
        return 80;
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
        return 2;
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
    public double getSpeed() {
        return 0.12;
    }

    @Override
    public void equip() {
        player.getInventory().setItem(0, ItemBuilder.material(Material.DIAMOND_HOE, 10100).name(rMsg("<gold><bold>test")).build());
    }

    @Skill(name = "雾障", id = 1)
    public void smoke() {
        if (ghost) {
            player.sendMessage(rMsg("你不能在二重世界使用此技能"));
            return;
        }
        Location location = player.getLocation();
        if (smoke_locs.size() == 2) {
            var l = smoke_locs.removeFirst();
            removeSkillLocation(l);
        }
        addSkillLocation(location, () -> {
            player.getWorld().spawnParticle(Particle.SMOKE, location, 10, 0.5, 0, 0.5, 0.1);
            if (skillFeature == 0) {
                location.getNearbyPlayers(2, 0.01, pl -> !pl.equals(player))
                        .forEach(pl -> {
                            PlayerData playerData = getPlayerData(pl);
                            if (captured.contains(pl)) return;
                            playerData.addEffect(new Giddy(pl, 40));
                            playerData.addEffect(new Exposure(pl, 300));
                            smoke_locs.remove(location);
                            removeSkillLocation(location);
                        });
            } else {
                location.getNearbyPlayers(2, 0.01, pl -> !pl.equals(player))
                        .forEach(pl -> {
                            if (captured.contains(pl)) return;
                            PlayerData playerData = getPlayerData(pl);
                            playerData.addEffect(new Slowness(pl, 100, 0));
                            smoke_locs.remove(location);
                            removeSkillLocation(location);
                        });
            }
        }, 1);
        smoke_locs.add(location);
        setSKillCD(1, 200);
    }

    @Skill(name = "鬼烟", id = 2)
    public void ghostSmoke() {
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 100, 0.5, 0, 0.5, 0.1);
        Location loc = new Location(player.getWorld(), 0, -1, 0);
        if (ghost) {
            //退出二重世界
            int tick = skillFeature == 1 ? 20 : 40;
            getPlayerData().addEffect(new Giddy(player, tick));
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    getGame().survivors.forEach(pl -> {
                        pl.showPlayer(plugin, player);
                        if (!captured.contains(pl)) player.showPlayer(plugin, pl);
                    }), tick);
            removeSkillLocation(loc);
        } else {
            //进入二重世界
            addSkillLocation(loc, () -> {
                if (skillFeature == 1) {
                    player.getWorld().spawnParticle(Particle.DUST, player.getLocation(), 1, 0, 0, 0, 0.1, new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1));
                    getGame().survivors.stream().filter(pl -> !captured.contains(pl)).min(Comparator.comparingDouble(pl -> pl.getLocation().distance(player.getLocation()))).ifPresent(pl -> {
                        var v = pl.getLocation().subtract(player.getLocation()).toVector().normalize().multiply(2);
                        for (Location location : LocationFactory.line(player.getLocation(), player.getLocation().add(v), 0.5)) {
                            player.spawnParticle(Particle.DUST, location, 1, 0, 0, 0, 0.1, new Particle.DustOptions(Color.fromRGB(0, 0, 255), 1));
                        }
                    });
                } else getGame().survivors.forEach(pl ->
                        player.spawnParticle(Particle.DUST, pl.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0.1, new Particle.DustOptions(Color.fromRGB(0, 0, 255), 1))
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
