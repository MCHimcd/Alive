package mc.alive.role.Butchers;

import mc.alive.role.Skill;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public  class Hunter extends Butcher {
    public Hunter(Player pl) {
        super(pl);
    }

    @Override
    public void equip() {

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
        return 100;
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
        player.sendMessage(Component.text("h"));
    }

}
