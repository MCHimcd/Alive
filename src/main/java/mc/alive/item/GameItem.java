package mc.alive.item;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.Range;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GameItem {
    public static final Map<String, Class<? extends GameItem>> registries = new HashMap<>();

    public static void register(Class<? extends GameItem> clazz) {
        registries.put(clazz.getSimpleName(), clazz);
    }

    /**
     * 80000-85000可使用 |
     * 84000-90000拾取时触发
     * 90001-100000普通
     */
    public abstract @Range(from = 80000, to = 100000) int customModelData();

    public abstract Component name();

    public abstract List<Component> lore();

    public abstract Material material();

    public abstract PickUp pickUp();

}

