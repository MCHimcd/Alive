package mc.alive;

import mc.alive.item.Air;
import mc.alive.item.GameItem;
import mc.alive.item.PickUp;
import mc.alive.item.pickup.PickUpHandler;
import mc.alive.item.usable.Usable;
import mc.alive.item.usable.gun.Gun;
import mc.alive.mechanism.Lift;
import mc.alive.mechanism.LiftDoor;
import mc.alive.menu.MainMenu;
import mc.alive.role.ChooseRole;
import mc.alive.role.Role;
import mc.alive.role.hunter.Alien;
import mc.alive.role.hunter.Hunter;
import mc.alive.tick.TickRunner;
import mc.alive.util.ItemBuilder;
import mc.alive.util.LocationFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.time.Duration;
import java.util.*;

import static mc.alive.Alive.*;
import static mc.alive.util.Message.rMsg;
import static org.bukkit.attribute.Attribute.*;


public final class Game {
    public static Team team_hunter;
    public static Team team_survivor;
    public static Game game = null;
    public final Map<Player, PlayerData> playerData = new HashMap<>();
    public final List<Player> survivors;
    public final Player hunter;
    public final Map<ItemStack, Usable> usable_items = new HashMap<>();
    public final Map<ItemStack, PickUpHandler> pickup_items = new HashMap<>();
    public final Map<Item, PickUp> item_on_ground = new HashMap<>();
    public final Map<BlockDisplay, Lift> lifts = new HashMap<>();
    public final Map<Block, LiftDoor> liftDoors = new HashMap<>();
    public final List<Entity> markers = new LinkedList<>();
    private final Map<ItemDisplay, Integer> fix_progress = new HashMap<>();
    public ChooseRole chooseRole;

    public Game(List<Player> players) {
        MainMenu.prepared_players.clear();
        chooseRole = new ChooseRole(players);
        players.forEach(player -> {
            player.getInventory().clear();
            player.closeInventory();
        });
        hunter = players.removeFirst();
        survivors = players;
        new BukkitRunnable() {
            @Override
            public void run() {
                chooseRole.nextChoose();
            }
        }.runTaskLater(plugin, 1);
    }

    public static boolean isStarted() {
        return game != null && game.chooseRole == null;
    }

    public void start() {
        new BukkitRunnable() {
            int t = 99;

            @Override
            public void run() {
                t++;
                if (t % 10 == 0) {
                    Component progressBar = rMsg("<gold>" + "■".repeat(t / 10) + "<white>" + "□".repeat(10 - t / 10));
                    Title title = Title.title(
                            rMsg("<rainbow> --游戏加载中--"),
                            progressBar,
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)
                    );
                    playerData.keySet().forEach(player -> {
                        player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                        player.showTitle(title);
                    });
                }
                if (t >= 100) {
                    chooseRole.roles.keySet().forEach(Entity::remove);
                    chooseRole = null;
                    playerData.forEach((player, playerData1) -> {
                        player.clearActivePotionEffects();
                        player.setHealth(20);
                        Role role = playerData1.getRole();
                        role.equip();
                        if (role instanceof Hunter h) //noinspection DataFlowIssue
                            player.getAttribute(PLAYER_ENTITY_INTERACTION_RANGE).setBaseValue(h.getAttackRange());
                    });
                    summonEntities();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    @SuppressWarnings({"DataFlowIssue", "unchecked"})
    private void summonEntities() {
        var world = Bukkit.getWorld("world");
        assert world != null;

        // 管道入口
        locations_config.getList("ducts.entrance").forEach(duct -> {
            var xyz = Arrays.stream(((String) duct).split(",")).mapToDouble(Double::parseDouble).toArray();
            world.spawn(new Location(world, xyz[0], xyz[1], xyz[2]), Marker.class, markers::add);
        });
        //垂直管道
        locations_config.getList("ducts.vertical").forEach(duct -> {
            var xyz = Arrays.stream(((String) duct).split(",")).mapToDouble(Double::parseDouble).toArray();
            world.spawn(new Location(world, xyz[0], xyz[1], xyz[2]), Marker.class, e -> {
                markers.add(e);
                e.addScoreboardTag("vertical_duct");
            });
        });

        // 维修
        locations_config.getList("fixes").forEach(fix -> {
            var xyz = Arrays.stream(((String) fix).split(",")).mapToDouble(Double::parseDouble).toArray();
            world.spawn(new Location(world, xyz[0], xyz[1], xyz[2]), ItemDisplay.class, id -> {
                id.setItemStack(new ItemStack(Material.FEATHER));
                fix_progress.put(id, 0);
            });
        });

        //可拾取物品
        var itemsInfo = (List<String>) locations_config.getList("items");
        for (String item : itemsInfo) {
            var info = item.split(" ");
            var x = Double.parseDouble(info[0]);
            var y = Double.parseDouble(info[1]);
            var z = Double.parseDouble(info[2]);
            var a = Integer.parseInt(info[3]);
            do {
                int finalA = a;
                Arrays.stream(info).skip(4).forEach(name -> {
                    var clazz = GameItem.registries.get(name);
                    if (clazz == null) return;
                    spawnItem(clazz, new Location(world, x, y, z), Math.min(finalA, 64));
                });
                a -= 64;
            } while (a >= 64);
        }

        //电梯
        var liftsInfo = (List<String>) locations_config.getList("lifts");
        for (String liftInfo : liftsInfo) {
            var info = liftInfo.split(" ");
            var xyz_lift = Arrays.stream(info[0].split(",")).mapToDouble(Double::parseDouble).toArray();
            var blockDisplay = world.spawn(new Location(world, xyz_lift[0], xyz_lift[1], xyz_lift[2]), BlockDisplay.class, bd -> {
                bd.setTransformation(new Transformation(
                        new Vector3f(),
                        new AxisAngle4f(),
                        new Vector3f(2, 0.3f, 2),
                        new AxisAngle4f()
                ));
                bd.setBlock(Bukkit.createBlockData(Material.IRON_BLOCK));
            });
            var lift = new Lift(blockDisplay, 3);
            lifts.put(blockDisplay, lift);
            //电梯门
            for (int i = 1; i < info.length; i++) {
                var xyz_liftDoor = Arrays.stream(info[i].split(",")).mapToInt(Integer::parseInt).toArray();
                var block = world.getBlockAt(xyz_liftDoor[0], xyz_liftDoor[1], xyz_liftDoor[2]);
                LiftDoor liftDoor = new LiftDoor(block, lift, i);
                if (i == 1) liftDoor.openDoor();
                else liftDoor.closeDoor();
                liftDoors.put(block, liftDoor);
            }
        }
    }

    public void spawnItem(Class<? extends GameItem> game_item, Location location, int amount) {
        location.getWorld().spawn(location, Item.class, item_entity -> {
            GameItem item = new Air();
            try {
                item = game_item.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                plugin.getLogger().warning(e.getLocalizedMessage());
            }

            var ib = ItemBuilder
                    .material(item.material())
                    .name(item.name())
                    .data(item.customModelData())
                    .lore(item.lore())
                    .amount(amount);
            ItemStack is;

            if (item instanceof Usable usableGameItem) {
                is = ib.unique().build();
                usable_items.put(is, usableGameItem);
            } else {
                is = ib.build();
            }

            if (item instanceof PickUpHandler pickUpHandler) {
                pickup_items.put(is, pickUpHandler);
            }

            if (item instanceof Gun gun) {
                usable_items.put(is, gun);
            }

            item_entity.setItemStack(is);
            item_entity.customName(is.displayName().append(amount == 1 ? Component.empty() : rMsg("*%d".formatted(amount))));
            item_entity.setCustomNameVisible(true);
            item_entity.setCanMobPickup(false);
            item_entity.setWillAge(false);
            item_entity.setOwner(new UUID(0, 0));
            item_on_ground.put(item_entity, item.pickUp());
        });
    }

    public void end(Player ender) {
        destroy();
        game = null;
        playerData.keySet().forEach(Game::resetPlayer);
        Bukkit.getScheduler().cancelTasks(plugin);
        new TickRunner().runTaskTimer(plugin, 0, 1);
        var world = Bukkit.getWorld("world");
        assert world != null;
        if (ender != null) world.sendMessage(ender.name().append(rMsg("强制结束了游戏")));
        else world.sendMessage(rMsg("游戏结束"));
    }

    public static void resetPlayer(Player player) {
        Map.of(
                GENERIC_MOVEMENT_SPEED, .1,
                GENERIC_ATTACK_DAMAGE, 1.0,
                GENERIC_MAX_ABSORPTION, 20.0,
                GENERIC_ATTACK_SPEED, 255.0,
                GENERIC_ATTACK_KNOCKBACK, -1.0,
                GENERIC_JUMP_STRENGTH, .42,
                PLAYER_ENTITY_INTERACTION_RANGE, 4.0
        ).forEach((key, value) -> {
            var a = player.getAttribute(key);
            assert a != null;
            a.setBaseValue(value);
        });

        var team = main_scoreboard.getPlayerTeam(player);
        if (team != null) {
            team.removePlayer(player);
        }

        Bukkit.getOnlinePlayers().forEach(player1 -> player1.showEntity(plugin, player));

        player.playerListName(player.name());
        player.displayName(player.name());
        player.setHealth(20);
        player.setAbsorptionAmount(0);
        player.setFoodLevel(20);
        player.setLevel(0);
        player.setExp(0);
        player.setViewDistance(1);
        player.teleport(new Location(player.getWorld(), -7.5, -59, 11.5));
        player.clearActivePotionEffects();
        player.getInventory().clear();

        if (game == null) {
            player.getInventory().setItem(8, ItemBuilder
                    .material(Material.CLOCK)
                    .data(20000)
                    .name(Component.text("主菜单", NamedTextColor.GOLD))
                    .build()
            );
            player.setGameMode(GameMode.ADVENTURE);
        } else {
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    public void destroy() {
        if (chooseRole != null) {
            chooseRole.roles.keySet().forEach(Entity::remove);
        }
        fix_progress.keySet().forEach(Entity::remove);
        item_on_ground.keySet().forEach(Entity::remove);
        for (Usable usableGameItem : usable_items.values()) {
            if (usableGameItem instanceof Gun gun) {
                gun.stopShoot(null);
            }
        }
        for (BlockDisplay blockDisplay : lifts.keySet()) {
            LocationFactory.replace2x2(blockDisplay.getLocation(), Material.AIR, BlockFace.SELF);
            blockDisplay.remove();
        }
        markers.forEach(Entity::remove);
    }

    public int fix(ItemDisplay id, int amount) {
        int currentProgress = fix_progress.getOrDefault(id, 0);
        int finalAmount = currentProgress + amount;

        if (finalAmount >= 400) {
            fix_progress.remove(id);
            id.remove();
            Bukkit.broadcast(rMsg("fix complete"));
        } else {
            fix_progress.put(id, finalAmount);
        }

        return finalAmount;
    }

    public void spawnBody(Player player) {
        markers.add(player.getWorld().spawn(player.getLocation().add(0, -1.5, 0), ArmorStand.class, armorStand -> {
            armorStand.setInvisible(true);
            armorStand.setMarker(true);
            armorStand.getEquipment().setHelmet(new ItemStack(Material.PLAYER_HEAD) {{
                editMeta(meta -> ((SkullMeta) meta).setOwningPlayer(player));
            }});
            armorStand.addScoreboardTag("body");
            //alien技能
            if (playerData.get(hunter).getRole() instanceof Alien alien) {
                Location location = armorStand.getLocation();
                alien.skill_locations.put(location, new BukkitRunnable() {
                    private int t = 0;

                    @Override
                    public void run() {
                        armorStand.getWorld().spawnParticle(Particle.DUST, location.clone().add(0, 2, 0), 100, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(Color.RED, 1f), true);
                        if (t++ == 300) {
                            alien.removeSkillLocation(location);
                        }
                    }
                }.runTaskTimer(plugin, 0, 1));
            }
        }));
    }


}