package mc.alive.util;

import mc.alive.game.PlayerData;
import mc.alive.game.TickRunner;
import mc.alive.role.Role;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static mc.alive.Alive.game;

public final class ChooseRole {
    private final List<Player> choosing = new ArrayList<>();
    public Player currentPlayer;
    public final Map<ItemDisplay, Integer> roles = new HashMap<>();
    public final List<Integer> remainedId = new ArrayList<>(IntStream.rangeClosed(200, 201).boxed().toList());

    public ChooseRole(List<Player> players) {
        choosing.addAll(players);
    }

    private void summonItemDisplay(boolean isHunter) {
        var world = Bukkit.getWorld("world");
        assert world != null;
        roles.clear();
        BiConsumer<ItemDisplay, ItemStack> init = (id, it) -> {
            id.setItemStack(it);
            id.setTransformation(new Transformation(
                    new Vector3f(0, -.5f, 0),
                    new Quaternionf(),
                    new Vector3f(1, 1, 1),
                    new Quaternionf()
            ));
            id.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
        };
        Supplier<Location> location = () -> {
            //todo
            return new Location(world, -2, -58, -4);
        };
        if (isHunter) {
            //狩猎者
            world.spawn(location.get(), ItemDisplay.class, id -> {
                init.accept(id, ItemCreator.create(Material.DIAMOND_HOE, 200).getItem());
                roles.put(id, 100);
            });
        } else {
            //幸存者
            remainedId.forEach(rid -> {
                switch (rid) {
                    case 200 -> world.spawn(location.get(), ItemDisplay.class, id -> {
                        init.accept(id, ItemCreator.create(Material.DIAMOND, 200).getItem());
                        roles.put(id, 200);
                    });
                    case 201 -> world.spawn(location.get(), ItemDisplay.class, id -> {
                        init.accept(id, ItemCreator.create(Material.DIAMOND, 200).getItem());
                        roles.put(id, 201);
                    });
                }
            });
        }
    }

    public void nextChoose() {
        var world = Bukkit.getWorld("world");
        assert world != null;
        //上一个
        if (currentPlayer != null) {
            roles.keySet().forEach(Entity::remove);
            currentPlayer.teleport(new Location(world, 10.5, -58, 10.5));
        }
        if (choosing.isEmpty()) {
            roles.keySet().forEach(Entity::remove);
            game.start();
            return;
        }
        //下一个
        currentPlayer = choosing.removeFirst();
        summonItemDisplay(currentPlayer.equals(game.hunter));
        currentPlayer.teleport(new Location(world, 0.5, -58, 0.5));
    }

    public boolean handleEvent(Player player) {
        if (player.equals(currentPlayer)) {
            //选role
            var td = TickRunner.chosen_item_display.get(player);
            if (td != null) {
                var role = roles.get(td);
                if (role != null) {
                    remainedId.remove(role);
                    game.playerData.put(player, new PlayerData(player, Objects.requireNonNull(Role.of(role, player))));
                    nextChoose();
                    return true;
                }
            }
        }
        return false;
    }
}
