package mc.alive.menu;

import mc.alive.role.Role;
import mc.alive.role.hunter.Hunter;
import org.bukkit.inventory.ItemStack;

import static mc.alive.Game.game;
import static mc.alive.util.Message.rMsg;

public class HChooseFeatureMenu extends SlotMenu {
    private final int page;
    private final Hunter hunter;

    public HChooseFeatureMenu(Role role, int page) {
        super(9, rMsg(switch (page) {
            case 0 -> "<red>选择红色特质";
            case 1 -> "<green>选择绿色特质";
            case 2 -> "<blue>选择蓝色特质";
            default -> "";
        }), role.getPlayer());
        this.hunter = (Hunter) role;
        this.page = page;
        int i = 0;
        if (page == 0) {
            for (ItemStack it : hunter.getRedFeatures()) {
                int finalI = i;
                setSlot(i++, it, (_, _) -> {
                    hunter.setRedFeature(finalI);
                    nextPage();
                    close = false;
                });
            }
        } else if (page == 1) {
            for (ItemStack it : hunter.getGreenFeatures()) {
                int finalI = i;
                setSlot(i++, it, (_, _) -> {
                    hunter.setGreenFeature(finalI);
                    nextPage();
                    close = false;
                });
            }
        } else if (page == 2) {
            for (ItemStack it : hunter.getBlueFeatures()) {
                int finalI = i;
                setSlot(i++, it, (_, _) -> {
                    hunter.setBlueFeature(finalI);
                    nextPage();
                    close = false;
                });
            }
        }
    }

    private void nextPage() {
        if (page == 2) {
            game.chooseRole.nextChoose();
            return;
        }
        player.openInventory(new HChooseFeatureMenu(hunter, page + 1).getInventory());
    }
}
