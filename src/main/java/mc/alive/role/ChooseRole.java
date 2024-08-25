package mc.alive.role;

import mc.alive.PlayerData;
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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.lang.Math.*;
import static mc.alive.Alive.plugin;
import static mc.alive.Game.game;

public final class ChooseRole {
    public final Map<ItemDisplay, Integer> roles = new HashMap<>();
    public final List<Integer> remainedId = new ArrayList<>(IntStream.rangeClosed(200, 202).boxed().toList());
    private final List<Player> choosing = new ArrayList<>();
    public Player currentPlayer;

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

        remainedId.remove(role);
        game.playerData.put(player, new PlayerData(player, Objects.requireNonNull(Role.of(role, player))));
        player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 1f);
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, 2f, 1f);
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

        if (isHunter) {
            // 狩猎者
            world.spawn(location.get(), ItemDisplay.class, id -> {
                init.accept(id, ItemBuilder.material(Material.DIAMOND_HOE, 200).build());
                roles.put(id, 100);
            });
        } else {
            // 幸存者
            remainedId.forEach(rid -> {
                Material material = switch (rid) {
                    case 200 -> Material.DIAMOND;
                    case 201 -> Material.IRON_INGOT;
                    case 202 -> Material.GOLD_INGOT;
                    default -> throw new IllegalArgumentException("Unexpected value: " + rid);
                };
                world.spawn(location.get(), ItemDisplay.class, id -> {
                    init.accept(id, ItemBuilder.material(material, rid).build());
                    roles.put(id, rid);
                });
            });
        }
    }
}