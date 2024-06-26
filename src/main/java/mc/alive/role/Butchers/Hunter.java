package mc.alive.role.Butchers;

import mc.alive.Alive;
import mc.alive.role.Skill;
import mc.alive.util.ItemCreator;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public  class Hunter extends Butcher {
    public Hunter(Player pl) {
        super(pl);
    }

    @Override
    public void equip() {
        player.getInventory().setItem(0, ItemCreator.create(Material.DIAMOND_HOE,10000).getItem());
    }

    @Override
    public double getSpeed() {
        return 2;
    }

    @Override
    public int getMaxLevel() {
        return 6;
    }

    @Override
    public double getMaxHealth() {
        return 1000;
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
        return "§c狩猎者";
    }

    @Skill(id = 1, name = "h")
    public void aaa() {
        player.sendMessage(Component.text("hhh"));
        Alive.game.sailors.forEach(player1 -> player1.damage(5));
    }

}
