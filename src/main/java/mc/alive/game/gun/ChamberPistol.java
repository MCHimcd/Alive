package mc.alive.game.gun;

import org.bukkit.inventory.ItemStack;

public class ChamberPistol extends Gun{
    public ChamberPistol(ItemStack item) {
        super(
                item,
                0.05f,
                BulletType.Chamber_Standard_Cartridge,
                10,
                10,
                300,
                20
        );
    }

}
