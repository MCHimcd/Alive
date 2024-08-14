package mc.alive.game.gun;

import mc.alive.game.item.ChamberStandardCartridge;
import org.bukkit.inventory.ItemStack;

public class ChamberShotgun extends Shotgun {
    public ChamberShotgun(ItemStack item) {
        super(
                item,
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
}
