package mc.alive.menu;

import mc.alive.StoredData;
import mc.alive.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static mc.alive.util.Message.rMsg;

public class SChooseFeatureMenu extends SlotMenu {
    public final static List<ItemStack> items = List.of(
            ItemBuilder.material(Material.ACACIA_BOAT).name(rMsg("test")).build(),
            ItemBuilder.material(Material.DAMAGED_ANVIL).name(rMsg("test2")).build()
    );

    public SChooseFeatureMenu(Player p) {
        super(27, rMsg("<gold>选择特质"), p);
        for (int i = 0; i < items.size(); i++) {
            int finalI = i;
            setSlot(i, items.get(i), (_, _) -> {
                StoredData.playerStoredData.get(p).setOption("feature", finalI);
                p.sendMessage(rMsg("<green>你选择了特质").append(items.get(finalI).displayName()));
            });
        }
    }
}
