package mc.alive.mechanism;

import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

import static mc.alive.Game.game;

public class SignalRepeater {
    private final ItemDisplay itemDisplay;
    private int progress = 0;
    private boolean fixed = false;

    public SignalRepeater(ItemDisplay itemDisplay) {
        this.itemDisplay = itemDisplay;
    }

    public void destroy(double percent) {
        progress = (int) (progress * percent);
        if (fixed) {
            fixed = false;
            itemDisplay.setItemStack(new ItemStack(Material.FEATHER));
        }
    }

    public void fix(int amount) {
        progress = Math.min(progress + amount, 400);

        if (progress == 400) {
            fixed = true;
            itemDisplay.setItemStack(new ItemStack(Material.ACACIA_BOAT));
            if (game.signal_repeaters.values().stream().filter(SignalRepeater::isFixed).count() >= 4) {
                game.intoSecondStage();
            }
        }
    }

    public boolean isFixed() {
        return fixed;
    }

    public int getProgress() {
        return progress;
    }
}
