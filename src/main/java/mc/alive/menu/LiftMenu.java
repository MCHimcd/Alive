package mc.alive.menu;

import mc.alive.mechanism.Lift;
import org.bukkit.entity.Player;

import java.util.List;

import static mc.alive.util.Message.rMsg;

public class LiftMenu extends SlotMenu {
    public LiftMenu(Player p, Lift lift) {
        super(9, rMsg("选择楼层"), p);
        List<Lift.Result> itemStacks = lift.getItemStacks();
        for (int i = 0; i < itemStacks.size(); i++) {
            var r = itemStacks.get(i);
            setSlot(i, r.item(), r.function());
        }
    }
}
