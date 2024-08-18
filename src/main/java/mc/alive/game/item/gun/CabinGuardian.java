package mc.alive.game.item.gun;

import mc.alive.game.item.ChamberStandardCartridge;
import net.kyori.adventure.text.Component;

import java.util.List;

import static mc.alive.util.Message.rMsg;

public class CabinGuardian extends Gun {
    public CabinGuardian() {
        super(
                3f,
                ChamberStandardCartridge.class,
                3,
                25,
                10,
                50
        );
    }

    @Override
    public int customModelData() {
        return 80003;
    }

    @Override
    public Component name() {
        return rMsg("冲锋枪");
    }

    @Override
    public List<Component> lore() {
        return List.of();
    }

}
