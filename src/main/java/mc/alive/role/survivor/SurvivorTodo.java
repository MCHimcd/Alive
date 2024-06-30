package mc.alive.role.survivor;

import mc.alive.role.Skill;
import mc.alive.util.ItemCreator;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SurvivorTodo extends Survivor {
    public SurvivorTodo(Player pl) {
        super(pl);
    }

    @Override
    public void equip() {
        player.getInventory().setItem(0, ItemCreator.create(Material.DIAMOND,10000).getItem());
    }


    @Override
    public double getSpeed() {
        return 1;
    }

    @Override
    public double getMaxHealth() {
        return 200;
    }

    @Override
    public int getIntelligence() {
        return 5;
    }

    @Override
    public int getMaxShield() {
        return 50;
    }

    @Override
    public int getStrength() {
        return 10;
    }

    @Override
    public String toString() {
        return "§a船员";
    }

    @Skill(id = 1, name = "位移")
    public void vector() {
        var Speed = player.getLocation().getDirection().normalize().multiply(3);
        player.playSound(player, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 1f);
        player.setVelocity(Speed);
    }

    @Skill(id = 2, name = "aaa")
    public void aaa() {
        player.sendMessage(Component.text("aaaa"));
    }

    @Skill(id = 3, name = "aaa",minLevel = 1)
    public void bbb() {
        player.sendMessage(Component.text("aaaa"));
    }
}
