package mc.alive.api;

import mc.alive.game.item.GameItem;

public final class Registries {
    public static void registerGameItem(Class<? extends GameItem> clazz) {
        GameItem.registries.put(clazz.getSimpleName(), clazz);
    }
}
