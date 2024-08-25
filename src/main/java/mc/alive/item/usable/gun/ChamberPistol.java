package mc.alive.item.usable.gun;

import mc.alive.item.ChamberStandardCartridge;
import net.kyori.adventure.text.Component;

import java.util.List;

import static mc.alive.util.Message.rMsg;

public class ChamberPistol extends Gun {
    public ChamberPistol() {
        super(
                10, 10, 20, 2f,
                500, ChamberStandardCartridge.class
        );
    }

    @Override
    public int customModelData() {
        return 80001;
    }

    @Override
    public Component name() {
        return rMsg("<red>标准舱室手枪");
    }

    @Override
    public List<Component> lore() {
        return List.of();
    }

}
