package mc.alive.game.gun;

import org.bukkit.inventory.ItemStack;

public class ChamberShotgun extends Shotgun {
    public ChamberShotgun(ItemStack item) {
        super(
                item,
                0.15f,
                BulletType.Chamber_Standard_Cartridge,
                5,
                10,
                2000,
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
