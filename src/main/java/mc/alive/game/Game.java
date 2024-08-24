package mc.alive.game;

import mc.alive.game.item.Air;
import mc.alive.game.item.GameItem;
import mc.alive.game.item.PickUp;
import mc.alive.game.item.gun.Gun;
import mc.alive.game.mechanism.Lift;
import mc.alive.game.mechanism.LiftDoor;
import mc.alive.game.role.Role;
import mc.alive.menu.MainMenu;
import mc.alive.tick.PlayerTickrunnable;
import mc.alive.tick.TickRunner;
import mc.alive.util.Factory;
import mc.alive.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.lang.Math.*;
import static mc.alive.Alive.*;
import static mc.alive.util.Message.rMsg;
import static org.bukkit.attribute.Attribute.*;


public class Game {
    public static Team team_hunter;
    public static Team team_survivor;
    public static Game game = null;
    public final Map<Player, PlayerData> playerData = new HashMap<>();
    public final List<Player> survivors;
    public final Player hunter;
    public final Map<ItemStack, Gun> guns = new HashMap<>();
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
                        playerData1.getRole().equip();
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
        config.getList("ducts").forEach(duct -> {
            var xyz = Arrays.stream(((String) duct).split(",")).mapToDouble(Double::parseDouble).toArray();
            world.spawn(new Location(world, xyz[0], xyz[1], xyz[2]), Marker.class, markers::add);
        });

        // 维修
        config.getList("fixes").forEach(fix -> {
            var xyz = Arrays.stream(((String) fix).split(",")).mapToDouble(Double::parseDouble).toArray();
            world.spawn(new Location(world, xyz[0], xyz[1], xyz[2]), ItemDisplay.class, id -> {
                id.setItemStack(new ItemStack(Material.FEATHER));
                fix_progress.put(id, 0);
            });
        });

        //可拾取物品
        var itemsInfo = (List<String>) config.getList("items");
        for (String item : itemsInfo) {
            var info = item.split(" ");
            var x = Double.parseDouble(info[0]);
            var y = Double.parseDouble(info[1]);
            var z = Double.parseDouble(info[2]);
            var a = Integer.parseInt(info[3]);
            do {
                int finalA = a;
                Arrays.stream(info).skip(4).forEach(name -> {
                    try {
                        spawnItem((Class<? extends GameItem>) Class.forName("mc.alive.game.item." + name), new Location(world, x, y, z), Math.min(finalA, 64));
                    } catch (ClassNotFoundException e) {
                        plugin.getLogger().warning(e.getLocalizedMessage());
                    }
                });
                a -= 64;
            } while (a >= 64);
        }

        //电梯
        var liftsInfo = (List<String>) config.getList("lifts");
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
            if (item instanceof Gun gun) {
                is = ib.unique().build();
                guns.put(is, gun);
            } else {
                is = ib.build();
            }
            item_entity.setItemStack(is);
            item_entity.customName(is.displayName().append(amount == 1 ? Component.empty() : rMsg("*%d".formatted(amount))));
            item_entity.setCustomNameVisible(true);
            item_entity.setCanMobPickup(false);
            item_entity.setWillAge(false);
            item_on_ground.put(item_entity, item.pickUp());
        });
    }

    public void end() {
        destroy();
        game = null;
        playerData.keySet().forEach(Game::resetPlayer);
        Bukkit.getScheduler().cancelTasks(plugin);
        new TickRunner().runTaskTimer(plugin, 0, 1);
        Bukkit.broadcast(Component.text("end"));
    }

    public static void resetPlayer(Player player) {
        Map.of(
                GENERIC_MOVEMENT_SPEED, .1,
                GENERIC_ATTACK_DAMAGE, 1.0,
                GENERIC_MAX_ABSORPTION, 20.0,
                GENERIC_ATTACK_SPEED, 255.0,
                GENERIC_ATTACK_KNOCKBACK, -1.0,
                GENERIC_JUMP_STRENGTH, .42
        ).forEach((key, value) -> {
            var a = player.getAttribute(key);
            assert a != null;
            a.setBaseValue(value);
        });

        var team = main_scoreboard.getPlayerTeam(player);
        if (team != null) {
            team.removePlayer(player);
        }

        player.playerListName(player.name());
        player.displayName(player.name());
        player.setHealth(20);
        player.setAbsorptionAmount(0);
        player.setFoodLevel(20);
        player.setLevel(0);
        player.setExp(0);
        player.setGameMode(GameMode.ADVENTURE);
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
        }
    }

    public void destroy() {
        if (chooseRole != null) {
            chooseRole.roles.keySet().forEach(Entity::remove);
        }
        fix_progress.keySet().forEach(Entity::remove);
        item_on_ground.keySet().forEach(Entity::remove);
        guns.values().forEach(gun -> gun.stopShoot(null));
        for (BlockDisplay blockDisplay : lifts.keySet()) {
            Factory.replace2x2(blockDisplay.getLocation(), Material.AIR, BlockFace.SELF);
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
        }));
    }

    public static final class ChooseRole {
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

            //itemDisplay初始化
            AtomicInteger i = new AtomicInteger(1);
            BiConsumer<ItemDisplay, ItemStack> init = (id, it) -> {
                id.setItemStack(it);
                id.setTransformation(new Transformation(
                        new Vector3f(0, -.5f, 0),
                        new Quaternionf(),
                        new Vector3f(1, 1, 1),
                        new Quaternionf(
                                0,
                                sin(toRadians(45 * i.get() - 90) * 0.5),
                                0,
                                cos(toRadians(45 * i.get() - 90) * 0.5)
                        )
                ));
                id.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
                Bukkit.getOnlinePlayers().forEach(player -> {
                    if (!player.equals(currentPlayer))
                        player.hideEntity(plugin, id);
                });
                i.incrementAndGet();
            };

            //获取itemDisplay位置
            Supplier<Location> location = () -> new Location(currentPlayer.getWorld(), -4, -58, -2)
                    .add(new Vector(2, 0, 0).rotateAroundY((float) toRadians(45 * i.get())));

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
}