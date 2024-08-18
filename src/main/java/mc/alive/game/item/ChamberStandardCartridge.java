package mc.alive.game.item;

import mc.alive.util.Message;
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
        return Message.rMsg("<red>船室标准弹");
    }

    @Override
    public List<Component> lore() {
        return List.of(Message.rMsg(""), Message.rMsg("<aqua><bold>船室标准弹"), Message.rMsg(" "), Message.rMsg("<gray>飞船上大多数枪通用的子弹,适用范围非常广"));
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