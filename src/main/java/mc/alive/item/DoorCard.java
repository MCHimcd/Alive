package mc.alive.item;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.Range;

import java.util.List;

import static mc.alive.util.Message.rMsg;

public class DoorCard extends GameItem {
    @Override
    public @Range(from = 80000, to = 100000) int customModelData() {
        return 90100;
    }

    @Override
    public Component name() {
        return rMsg("密码卡");
    }

    @Override
    public List<Component> lore() {
        return List.of();
    }

    @Override
    public Material material() {
        return Material.PAPER;
    }

    @Override
    public PickUp pickUp() {
        return PickUp.SURVIVOR;
    }
}
