package mc.alive.game.item;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;

public class ChamberStandardCartridge extends GameItem {

    @Override
    public int customModelData() {
        return 90001;
    }

    @Override
    public Component name() {
        return null;
    }

    @Override
    public List<Component> lore() {
        return List.of();
    }

    @Override
    public Material material() {
        return Material.DIAMOND;
    }

    @Override
    public PickUp pickUp() {
        return PickUp.SURVIVOR;
    }


}
