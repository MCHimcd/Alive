package mc.alive.role.survivor;

import io.papermc.paper.entity.LookAnchor;
import mc.alive.Alive;
import mc.alive.effect.Giddy;
import mc.alive.effect.Speed;
import mc.alive.role.Skill;
import mc.alive.util.LocationFactory;
import mc.alive.util.ItemBuilder;
import mc.alive.util.Message;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import static mc.alive.Game.game;
import static mc.alive.PlayerData.setSkillCD;

public class Jack extends Survivor {
    public Jack(Player pl) {
        super(pl);
    }

    @Override
    public int getRoleID() {
        return 200;
    }

    @Override
    public int getStrength() {
        return 10;
    }

    @Override
    public double getAttackCD() {
        return 0;
    }

    @Override
    public double getSpeed() {
        return 0.1;
    }

    @Override
    public double getMaxHealth() {
        return 20;
    }

    @Override
    public void equip() {
        player.getInventory().setItem(0, ItemBuilder.material(Material.DIAMOND, 10200).name(Message.rMsg("<gold><bold>个人终端")).build());
    }

    @Override
    public int getMaxShield() {
        return 20;
    }

    @Override
    public int getFixSpeed() {
        return 5;
    }

    @Skill(id = 1, name = "§b肾上腺素")
    public void speed() {
        //2s 加速2
        getPlayerData().addEffect(new Speed(player, 40, 1));
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().clone().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0, null, true);

        //清除jack 周围4格内  hunter的location
        game.playerData.get(game.hunter).getRole().skill_locations.entrySet().removeIf(entry -> {
            if (entry.getKey().distance(player.getEyeLocation()) <= 4) {
                LocationFactory.line(player.getEyeLocation().subtract(0, 1, 0), entry.getKey(), 0.5).forEach(location ->
                        player.spawnParticle(
                                Particle.FLAME,
                                location.subtract(0, 1, 0),
                                1,
                                0,
                                0,
                                0,
                                0,
                                null,
                                true)
                );
                var bukkitTask = entry.getValue();
                if (bukkitTask != null && !bukkitTask.isCancelled()) bukkitTask.cancel();
                return true;
            }
            return false;
        });

        setSkillCD(player, 1, 300);
    }

    @Skill(id = 2, name = "§9窥视")
    public void see() {
        new BukkitRunnable() {
            int time = 0;

            @Override
            public void run() {
                if (time++ == 40) {
                    //noinspection UnstableApiUsage
                    player.lookAt(game.hunter.getLocation(), LookAnchor.EYES);
                    LocationFactory.line(player.getLocation(), game.hunter.getLocation(), 0.5).forEach(location ->
                            player.spawnParticle(Particle.DUST, location, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 1f)));
                    cancel();
                } else {
                    player.getWorld().spawnParticle(Particle.DUST, player.getEyeLocation(), 5, 0.3, 0.3, 0.3, 0, new Particle.DustOptions(Color.RED, .5f), true);
                }
            }
        }.runTaskTimer(Alive.plugin, 0, 1);
        player.playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 1f, .5f);
        getPlayerData().addEffect(new Giddy(player, 40));
        setSkillCD(player, 2, 140);
    }
}