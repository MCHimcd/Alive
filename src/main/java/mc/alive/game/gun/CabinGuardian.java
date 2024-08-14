package mc.alive.game.gun;

import mc.alive.game.item.ChamberStandardCartridge;
import org.bukkit.inventory.ItemStack;

public class CabinGuardian extends Gun {
    public CabinGuardian(ItemStack item) {
        super(
                item,
                3f,
                ChamberStandardCartridge.class,
                10,
                30,
                10,
                50
        );
    }
}
