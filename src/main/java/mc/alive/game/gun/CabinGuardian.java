package mc.alive.game.gun;

import org.bukkit.inventory.ItemStack;

public class CabinGuardian extends Gun {
    public CabinGuardian(ItemStack item) {
        super(
                item,
                2f,//0.05
                Gun.BulletType.Chamber_Standard_Cartridge,
                3,
                25,
                20,
                30
        );
    }
}
