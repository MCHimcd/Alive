package mc.alive.role.survivor;

import mc.alive.role.Skill;
import mc.alive.util.ItemCreator;
import mc.alive.util.Message;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class Mike extends Survivor{
    public Mike(Player pl) {
        super(pl);
    }

    @Override
    public void equip() {
        player.getInventory().setItem(0, ItemCreator.material(Material.DIAMOND,10200).name(Message.rMsg("<gold><bold>个人终端")).create());
    }


    @Override
    public double getSpeed() {
        return 0.1;
    }

    @Override
    public double getAttackCD() {
        return 0;
    }

    @Override
    public double getMaxHealth() {
        return 20;
    }

    @Override
    public int getIntelligence() {
        return 5;
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
    public String toString() {
        return names.get(201);
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

}