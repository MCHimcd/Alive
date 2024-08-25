package mc.alive.game.item.usable.gun;

import mc.alive.game.item.ChamberStandardCartridge;
import net.kyori.adventure.text.Component;

import java.util.List;

import static mc.alive.util.Message.rMsg;

public class ChamberShotgun extends Shotgun {
    public ChamberShotgun() {
        super(
                15f,
                ChamberStandardCartridge.class,
                5,
                5,
                1000,
                60
        );
    }

    @Override
    double getSpread() {
        return 0.2;
    }

    @Override
    int getBulletsCount() {
        return 8;
    }

    @Override
    public int customModelData() {
        return 80002;
    }

    @Override
    public Component name() {
        return rMsg("霰弹枪");
    }

    @Override
    public List<Component> lore() {
        return List.of();
    }

}
