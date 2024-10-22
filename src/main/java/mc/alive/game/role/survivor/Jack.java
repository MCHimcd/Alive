package mc.alive.game.role.survivor;

import io.papermc.paper.entity.LookAnchor;
import mc.alive.Alive;
import mc.alive.game.effect.Effect;
import mc.alive.game.role.Skill;
import mc.alive.util.Factory;
import mc.alive.util.ItemBuilder;
import mc.alive.util.Message;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;

import static mc.alive.game.Game.game;
import static mc.alive.game.PlayerData.setSkillCD;

public class Jack extends Survivor {
    public Jack(Player pl) {
        super(pl);
    }

    @Override
    public int getMaxShield() {
        return 20;
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
    public int getFixSpeed() {
        return 5;
    }

    @Override
    public void equip() {
        player.getInventory().setItem(0, ItemBuilder.material(Material.DIAMOND, 10200).name(Message.rMsg("<gold><bold>个人终端")).build());
    }

    @Override
    public String toString() {
        return names.get(200);
    }

    @Skill(id = 1, name = "§b肾上腺素")
    public void speed() {
        //2s 加速2
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false));
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().clone().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0, null, true);

        //在这里写一个清除jack 周围4格内  hunter的location
        var locs = game.playerData.get(game.hunter).getRole().skill_locations;
        var remove = new ArrayList<Location>();
        for (Location loc : locs.keySet()) {
            if (loc.distance(player.getLocation()) <= 4) {
                Factory.line(player.getEyeLocation().subtract(0, 1, 0), loc, 0.5).forEach(location ->
                        player.spawnParticle(
                                Particle.FLAME,
                                location.subtract(0, 1, 0),
                                1,
                                0,
                                0,
                                0,
                                0,
                                null,
                                true
                        ));
                remove.add(loc);
            }
        }
        remove.forEach(loc -> {
            locs.get(loc).cancel();
            locs.remove(loc);
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
                    Factory.line(player.getLocation(), game.hunter.getLocation(), 0.5).forEach(location ->
                            player.spawnParticle(Particle.DUST, location, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 1f)));
                    cancel();
                } else {
                    player.getWorld().spawnParticle(Particle.DUST, player.getEyeLocation(), 5, 0.3, 0.3, 0.3, 0, new Particle.DustOptions(Color.RED, .5f), true);
                }
            }
        }.runTaskTimer(Alive.plugin, 0, 1);
        player.playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 1f, .5f);
        getPlayerData().addEffect(Effect.giddy(player, 40));
        setSkillCD(player, 2, 140);
    }
}