package mc.alive.game.gun;

import org.bukkit.inventory.ItemStack;

public class ChamberPistol extends Gun{
    public ChamberPistol(ItemStack item) {
        super(
                item,
                2f,//0.05
                BulletType.Chamber_Standard_Cartridge,
                10,
                10,
                500,
                20
        );
    }

}