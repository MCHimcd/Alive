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
        this(role, page, null);
    }

    public HChooseFeatureMenu(Role role, int page, ItemStack chosen) {
        super(
                page == 0 ? 9 : 27,
                rMsg(page == 0 ? "<gray>选择<red>红色<gray>特质" : "<gray>选择<green>绿色<gray>和<blue>蓝色<gray>特质"),
                role.getPlayer()
        );
        this.hunter = (Hunter) role;
        this.page = page;
        int i = 0;
        if (page == 0) {
            for (ItemStack it : hunter.getRedFeatures()) {
                int finalI = i;
                setSlot(i++, it, (_, _) -> {
                    hunter.setSkillFeature(finalI);
                    nextPage(null);
                    close = false;
                });
            }
        } else if (page == 1 || page == 2) {
            for (ItemStack it : Hunter.getOtherFeatures()) {
                if (chosen != null && chosen.equals(it)) continue;
                int finalI = i;
                setSlot(i++, it, (_, _) -> {
                    hunter.setOtherFeature(finalI);
                    nextPage(it);
                    close = false;
                });
            }
            int ii = i;
            for (ItemStack it : Hunter.getPursuitFeatures()) {
                if (chosen != null && chosen.equals(it)) continue;
                int finalI = i - ii;
                setSlot(i++, it, (_, _) -> {
                    hunter.setPursuitFeature(finalI);
                    nextPage(it);
                    close = false;
                });
            }
        }
    }

    private void nextPage(ItemStack chosen) {
        if (page == 2) {
            game.chooseRole.nextChoose();
            return;
        }
        player.openInventory(new HChooseFeatureMenu(hunter, page + 1, chosen).getInventory());
    }
}
