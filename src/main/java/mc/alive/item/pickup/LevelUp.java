package mc.alive.item.pickup;

import mc.alive.PlayerData;
import mc.alive.item.GameItem;
import mc.alive.item.PickUp;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

import static mc.alive.util.Message.rMsg;

public class LevelUp extends GameItem implements PickUpHandler {
    @Override
    public int customModelData() {
        return 85001;
    }

    @Override
    public Component name() {
        return rMsg("H升级");
    }

    @Override
    public List<Component> lore() {
        return List.of();
    }

    @Override
    public Material material() {
        return Material.NETHER_STAR;
    }

    @Override
    public PickUp pickUp() {
        return PickUp.HUNTER;
    }

    @Override
    public boolean handlePickUp(Player player) {
        PlayerData.of(player).tryLevelUp();
        return true;
    }
}
