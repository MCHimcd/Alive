package mc.alive.game.role.hunter;

import mc.alive.game.role.Skill;
import mc.alive.util.ItemBuilder;
import mc.alive.util.Message;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class Alien extends Hunter {
    public Alien(Player pl) {
        super(pl);
    }

    @Override
    public void equip() {
        player.getInventory().setItem(0, ItemBuilder.material(Material.DIAMOND_HOE, 10100).name(Message.rMsg("<red><bold>手镰")).build());
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
        return 3;
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

    //吐出一滩粘液 减速范围内的人
    @Skill(id = 1, name = "粘液")
    public void slime() {

    }

    @Skill(id = 2, name = "粘液")

    public void a() {
        //todo
    }

    @Skill(id = 3, name = "粘液", minLevel = 1)
    public void b() {
        //todo
    }

    @Skill(id = 4, name = "粘液", minLevel = 2)
    public void c() {
        //todo
    }


}
