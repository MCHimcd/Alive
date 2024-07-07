package mc.alive.role.hunter;

import mc.alive.Alive;
import mc.alive.game.PlayerData;
import mc.alive.role.Skill;
import mc.alive.util.ItemCreator;
import mc.alive.util.Message;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public  class HunterTodo extends Hunter {
    public HunterTodo(Player pl) {
        super(pl);
    }

    @Override
    public void equip() {
        player.getInventory().setItem(0, ItemCreator.create(Material.DIAMOND_HOE,10100).name(Message.rMsg("<red><bold>手镰")).getItem());
    }

    @Override
    public double getSpeed() {
        return 0.2;
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

    @Skill(id = 1, name = "收割")
    public void aaa() {
        Alive.game.survivors.forEach(player1 -> PlayerData.getPlayerData(player1).damageOrHeal(10));
    }

}
