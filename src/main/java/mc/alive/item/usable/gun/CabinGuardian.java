package mc.alive.item.usable.gun;

import mc.alive.item.ChamberStandardCartridge;
import mc.alive.util.Message;
import net.kyori.adventure.text.Component;

import java.util.List;

import static mc.alive.util.Message.rMsg;

public class CabinGuardian extends Gun {
    public CabinGuardian() {
        super(
                3, 25, 50, 3f,
                10, ChamberStandardCartridge.class
        );
    }

    @Override
    public int customModelData() {
        return 80003;
    }

    @Override
    public Component name() {
        return rMsg("<red>冲锋枪");
    }

    @Override
    public List<Component> lore() {
        return Message.convertMsg(List.of(
                " ",
                "<aqua><bold>         冲锋枪",
                "<gray>",
                "",
                "<gray> [枪械]"));
    }

}
