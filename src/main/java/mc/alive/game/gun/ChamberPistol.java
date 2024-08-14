package mc.alive.game.gun;

import mc.alive.game.game_item.ChamberStandardCartridge;
import org.bukkit.inventory.ItemStack;

public class ChamberPistol extends Gun {
    public ChamberPistol(ItemStack item) {
        super(
                item,
                2f,
                ChamberStandardCartridge.class,
                10,
                10,
                500,
                20
        );
    }

}
