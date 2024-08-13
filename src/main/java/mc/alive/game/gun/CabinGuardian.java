package mc.alive.game.gun;

import org.bukkit.inventory.ItemStack;

public class CabinGuardian extends Gun {
    public CabinGuardian(ItemStack item) {
        super(
                item,
                3f,
                BulletType.Chamber_Standard_Cartridge,
                10,
                30,
                100,
                50
        );
    }
}
