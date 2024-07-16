package mc.alive.role.hunter;

import mc.alive.Alive;
import mc.alive.game.effect.Effect;
import mc.alive.role.Skill;
import mc.alive.util.Factory;
import mc.alive.util.ItemCreator;
import mc.alive.util.Message;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;

import static mc.alive.game.PlayerData.getPlayerData;
import static mc.alive.game.PlayerData.setSkillCD;

public class Alien extends Hunter {
    public Alien(Player pl) {
        super(pl);
    }

    @Override
    public void equip() {
        player.getInventory().setItem(0, ItemCreator.material(Material.DIAMOND_HOE, 10100).name(Message.rMsg("<red><bold>手镰")).create());
    }

    @Override
    public double getSpeed() {
        return 0.1;
    }

    @Override
    public double getAttackCD() {
        return 1.8;
    }

    @Override
    public int getMaxLevel() {
        return 6;
    }

    @Override
    public double getRange() {
        return 4;
    }

    @Override
    public double getMaxHealth() {
        return 50;
    }

    @Override
    public int getIntelligence() {
        return 1;
    }

    @Override
    public int getStrength() {
        return 20;
    }

    @Override
    public String toString() {
        return names.get(100);
    }

    @Skill
    public void attack() {
        new BukkitRunnable() {
            @Override
            public void run() {
                List<Location> locations = Factory.attackRange(getRange(), player);
                HashSet<Player> players = new HashSet<>();
                locations.forEach(location -> {
                    player.getWorld().spawnParticle(Particle.DUST, location, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 1f), true);
                    location.getNearbyPlayers(2, player1 -> !player1.equals(player) && player1.getBoundingBox().contains(location.toVector()) && players.add(player1))
                            .forEach(player2 -> {
                                var pd = getPlayerData(player2);
                                pd.damageOrHeal(getStrength());
                            });
                });
            }
        }.runTaskLater(Alive.plugin, 10);
        getPlayerData(player).addEffect(Effect.giddy(player, 10));
        setSkillCD(player, 0, 50);
    }

    //吐出一滩粘液 减速范围内的人
    @Skill(id = 1, name = "粘液")
    public void slime() {

    }

    @Skill(id = 2, name = "粘液")

    public void a() {
        ;
    }

    @Skill(id = 3, name = "粘液", minLevel = 1)
    public void b() {
        ;
    }

    @Skill(id = 4, name = "粘液", minLevel = 2)
    public void c() {
        ;
    }


}
