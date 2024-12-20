package mc.alive.role;

import mc.alive.PlayerData;
import mc.alive.menu.HChooseFeatureMenu;
import mc.alive.tick.PlayerTickrunnable;
import mc.alive.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.lang.Math.*;
import static java.util.Map.entry;
import static mc.alive.Alive.plugin;
import static mc.alive.Game.game;

public final class ChooseRole {
    private final static Map<Integer, Material> displayItems = Map.ofEntries(
            entry(100, Material.DIAMOND_HOE),
            entry(200, Material.DIAMOND),
            entry(201, Material.IRON_INGOT),
            entry(202, Material.GOLD_INGOT)
    );
    public final Map<ItemDisplay, Integer> roles = new HashMap<>();
    private final List<Integer> remainedId_S = new ArrayList<>(IntStream.rangeClosed(200, 202).boxed().toList());
    private final List<Integer> remainedId_H = new ArrayList<>(IntStream.rangeClosed(100, 100).boxed().toList());
    private final List<Player> choosing = new ArrayList<>();
    private Player currentPlayer;

    public ChooseRole(List<Player> players) {
        choosing.addAll(players);
        players.forEach(player -> {
            player.displayName(Component.empty());
            player.playerListName(Component.empty());
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 0, false, false));
        });
    }

    public boolean handleEvent(Player player) {
        if (!player.equals(currentPlayer)) return false;

        var td = PlayerTickrunnable.chosen_item_display.get(player);
        if (td == null) return false;

        var role = roles.get(td);
        if (role == null) return false;

        Role r = Role.of(role, player);
        assert r != null;
        game.playerData.put(player, new PlayerData(player, r));
        player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 1f);
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, 2f, 1f);

        if (role >= 200) remainedId_S.remove(role);
        else {
            // 狩猎者
            player.openInventory(new HChooseFeatureMenu(r, 0).getInventory());
            return true;
        }
        nextChoose();
        return true;
    }

    /**
     * 下一个玩家选择角色
     */
    public void nextChoose() {
        var world = Bukkit.getWorld("world");
        assert world != null;

        //上一个
        if (currentPlayer != null) {
            roles.keySet().forEach(Entity::remove);
            currentPlayer.teleport(new Location(world, 10.5, -58, 10.5));
        }

        //结束判断
        if (choosing.isEmpty()) {
            roles.keySet().forEach(Entity::remove);
            game.start();
            return;
        }

        //下一个
        currentPlayer = choosing.removeFirst();
        summonItemDisplay(currentPlayer.equals(game.hunter));
        currentPlayer.teleport(new Location(world, -4.5, -58, -1.5));
    }

    /**
     * 生成对应职业的供玩家选择的ItemDisplay
     */
    private void summonItemDisplay(boolean isHunter) {
        roles.clear();
        var world = Bukkit.getWorld("world");
        assert world != null;

        // itemDisplay初始化
        AtomicInteger i = new AtomicInteger(1);
        BiConsumer<ItemDisplay, ItemStack> init = (id, it) -> {
            id.setItemStack(it);
            float angle = (float) toRadians(45 * i.getAndIncrement() - 90);
            id.setTransformation(new Transformation(
                    new Vector3f(0, -.5f, 0),
                    new Quaternionf(),
                    new Vector3f(1, 1, 1),
                    new Quaternionf(0, (float) sin(angle * 0.5), 0, (float) cos(angle * 0.5))
            ));
            id.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (!player.equals(currentPlayer)) player.hideEntity(plugin, id);
            });
        };

        // 获取itemDisplay位置
        Supplier<Location> location = () -> {
            int currentIndex = i.get() - 1;
            return new Location(currentPlayer.getWorld(), -4, -58, -2)
                    .add(new Vector(2, 0, 0).rotateAroundY((float) toRadians(45 * currentIndex)));
        };

        (isHunter ? remainedId_H : remainedId_S).forEach(remainedId ->
                world.spawn(location.get(), ItemDisplay.class, id -> {
                    init.accept(id, ItemBuilder.material(displayItems.get(remainedId), remainedId).build());
                    roles.put(id, remainedId);
                }));
    }
}