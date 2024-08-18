package mc.alive.game.item.gun;

import mc.alive.game.item.ChamberStandardCartridge;
import net.kyori.adventure.text.Component;

import java.util.List;

import static mc.alive.util.Message.rMsg;

public class ChamberPistol extends Gun {
    public ChamberPistol() {
        super(
                2f,
                ChamberStandardCartridge.class,
                10,
                10,
                500,
                20
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
