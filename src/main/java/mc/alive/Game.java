package mc.alive;

import mc.alive.item.Air;
import mc.alive.item.GameItem;
import mc.alive.item.PickUp;
import mc.alive.item.pickup.PickUpHandler;
import mc.alive.item.usable.Usable;
import mc.alive.mechanism.*;
import mc.alive.menu.MainMenu;
import mc.alive.role.ChooseRole;
import mc.alive.role.Role;
import mc.alive.role.hunter.Hunter;
import mc.alive.role.hunter.SmokeGhost;
import mc.alive.role.survivor.Dealt;
import mc.alive.role.survivor.Mike;
import mc.alive.role.survivor.Survivor;
import mc.alive.tick.TickRunner;
import mc.alive.util.ItemBuilder;
import mc.alive.util.LocationFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.*;

import static mc.alive.Alive.*;
import static mc.alive.util.Message.rMsg;
import static org.bukkit.attribute.Attribute.*;


@SuppressWarnings({"unchecked", "DataFlowIssue"})
public final class Game {
    public static Team team_hunter;
    public static Team team_survivor;
    public static Game game = null;
    public final Map<Location, Door> doors = new HashMap<>();
    public final Map<Location, Barrier> barriers = new HashMap<>();
    public final Map<Player, PlayerData> playerData = new HashMap<>();
    public final List<Player> survivors;
    public final Map<ItemStack, Usable> usable_items = new HashMap<>();
    public final Map<ItemStack, PickUpHandler> pickup_items = new HashMap<>();
    public final Map<Item, PickUp> item_on_ground = new HashMap<>();
    public final Map<BlockDisplay, Lift> lifts = new HashMap<>();
    public final Map<Block, LiftDoor> liftDoors = new HashMap<>();
    public final List<Entity> markers = new LinkedList<>();
    public final Map<Entity, List<String>> pickable_bodies = new HashMap<>();
    public final Map<ItemDisplay, SignalRepeater> signal_repeaters = new HashMap<>();
    private final Random random = new Random();
    private final World world = Bukkit.getWorld("world");
    public boolean isDebugging = false;
    public Player hunter;
    public BukkitTask pause_task = null;
    public boolean isPaused = false;
    public ChooseRole chooseRole;
    private int survivorScore = 0;
    private int hunterScore = 0;

    public Game(List<Player> players) {
        this(players, false);
    }

    public Game(List<Player> players, boolean debug) {
        MainMenu.prepared_players.clear();
        if (debug) {
            isDebugging = true;
            Player p1 = players.getFirst();
            SmokeGhost hunter = new SmokeGhost(p1);
            hunter.setOtherFeature(0);
            hunter.setPursuitFeature(0);
            hunter.setSkillFeature(0);
            playerData.put(p1, new PlayerData(p1, hunter));
            this.hunter = p1;
            Player p2 = players.get(1);
            playerData.put(p2, new PlayerData(p2, new Mike(p2)));
            survivors = new ArrayList<>();
            survivors.add(p2);
            Player p3 = players.get(2);
            if (p3 != null) {
                playerData.put(p3, new PlayerData(p3, new Dealt(p3)));
                survivors.add(p3);
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    start(true);
                }
            }.runTaskLater(plugin, 1);
        } else {
            chooseRole = new ChooseRole(players);
            players.forEach(player -> {
                player.getInventory().clear();
                player.closeInventory();
            });
            hunter = players.removeLast();
            survivors = players;
            new BukkitRunnable() {
                @Override
                public void run() {
                    chooseRole.nextChoose();
                }
            }.runTaskLater(plugin, 1);
        }
    }

    public void start(boolean debug) {
        if (debug) {
            playerData.forEach((player, playerData1) -> {
                player.clearActivePotionEffects();
                player.setHealth(20);
                Role role = playerData1.getRole();
                role.equip();
                if (role instanceof Hunter h) //noinspection DataFlowIssue
                    player.getAttribute(ENTITY_INTERACTION_RANGE).setBaseValue(h.getAttackRange());
            });
            summonEntities();
        } else {
            new BukkitRunnable() {
                int t = 0;

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
                                player.getAttribute(ENTITY_INTERACTION_RANGE).setBaseValue(h.getAttackRange());
                        });
                        summonEntities();
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
        }
    }

    /**
     * 生成由locations.yml决定的固定位置实体
     */
    private void summonEntities() {

        assert world != null;

        spawnDucts();
        spawnSignalRepeaters();
        spawnPickableItems();
        spawnDoors();
        spawnLifts();
        spawnBodies();
        spawnBarriers();
        GhostDom.summon();
    }

    private void spawnDucts() {
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
    }

    private void spawnSignalRepeaters() {
        // 维修
        List<String> signalRepeaters = (List<String>) locations_config.getList("signal_repeaters");
        Collections.shuffle(signalRepeaters);
        signalRepeaters.stream().limit(6).forEach(fix -> {
            var xyz = Arrays.stream(fix.split(",")).mapToDouble(Double::parseDouble).toArray();
            world.spawn(new Location(world, xyz[0], xyz[1], xyz[2]), ItemDisplay.class, id -> {
                id.setItemStack(new ItemStack(Material.FEATHER));
                signal_repeaters.put(id, new SignalRepeater(id));
            });
        });
    }

    private void spawnPickableItems() {
        //可拾取物品
        var itemsInfo = (List<String>) locations_config.getList("items");
        for (String item : itemsInfo) {
            var info = item.split(" ");
            var x = Double.parseDouble(info[0]);
            var y = Double.parseDouble(info[1]);
            var z = Double.parseDouble(info[2]);
            Arrays.stream(info).skip(3).forEach(name -> {
                int i = name.indexOf("*");
                int amount = i == -1 ? 1 : Integer.parseInt(name.substring(i + 1));
                name = i == -1 ? name : name.substring(0, i);
                int key_id = -1;
                if (name.startsWith("DoorCard")) {
                    key_id = Integer.parseInt(name.substring(8));
                    name = "DoorCard";
                }
                var clazz = GameItem.registries.get(name);
                if (clazz != null) {
                    do {
                        if (key_id != -1) spawnItem(clazz, new Location(world, x, y, z), Math.min(amount, 64), key_id);
                        else spawnItem(clazz, new Location(world, x, y, z), Math.min(amount, 64));
                        amount -= 64;
                    } while (amount > 64);
                }
            });
        }
    }

    public void spawnItem(Class<? extends GameItem> game_item, Location location, int amount) {
        spawnItem(game_item, location, amount, null);
    }

    /**
     * 生成可右键拾取的物品
     * @param game_item 类
     * @param location 位置
     * @param amount 数量
     * @param data CustomModelData
     * @return 生成的实体
     */
    public Item spawnItem(Class<? extends GameItem> game_item, Location location, int amount, Integer data) {
        return location.getWorld().spawn(location, Item.class, item_entity -> {
            GameItem item = new Air();
            try {
                item = game_item.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                plugin.getLogger().warning(e.getLocalizedMessage());
            }

            var ib = ItemBuilder
                    .material(item.material())
                    .name(item.name())
                    .modelData(item.customModelData())
                    .lore(item.lore())
                    .amount(amount);
            ItemStack is;

            if (item instanceof Usable usableGameItem) {
                is = ib.unique().build();
                usable_items.put(is, usableGameItem);
            } else {
                is = ib.build();
            }

            if (data != null) {
                is.editMeta(meta -> {
                    meta.addAttributeModifier(LUCK, new AttributeModifier(Door.key_id, data, AttributeModifier.Operation.ADD_NUMBER));
                    //noinspection DataFlowIssue
                    meta.displayName(meta.displayName().append(rMsg(" #%d".formatted(data))));
                });
            }

            if (item instanceof PickUpHandler pickUpHandler) {
                pickup_items.put(is, pickUpHandler);
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

    private void spawnDoors() {
        //密码门
        var doorsInfo = (List<String>) locations_config.getList("doors");
        int id_g = 0;
        Door[] r_doors = new Door[4];
        for (String doorInfo : doorsInfo) {
            var info = doorInfo.split(" ");
            var xyz = Arrays.stream(info[0].split(",")).mapToInt(Integer::parseInt).toArray();
            Location start = new Location(world, xyz[0], xyz[1], xyz[2]);
            Door door = new Door(start, info[1].equals("N") ? BlockFace.EAST : BlockFace.NORTH, ++id_g);
            doors.put(start, door);
            if (id_g <= 4) r_doors[id_g - 1] = door;
            if (id_g == 5) door.open();
        }
        //开局4个门中随机一个没锁
        r_doors[random.nextInt(4)].open();
    }

    private void spawnLifts() {
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

    private void spawnBodies() {
        //尸体
        Map<Location, List<String>> final_names = new HashMap<>();
        Map<String, List<Location>> r_locs = new HashMap<>(); // 没个门卡随机一个位置
        var bodiesInfo = (List<String>) locations_config.getList("bodies.locs");
        for (String bodyInfo : bodiesInfo) {
            var info = bodyInfo.split(" ");
            var xyz = Arrays.stream(info[0].split(",")).mapToDouble(Double::parseDouble).toArray();
            Location location = new Location(world, xyz[0], xyz[1], xyz[2]);
            List<String> names = new ArrayList<>();
            Arrays.stream(info).skip(1).forEach(name -> {
                if (name.matches("^DoorCard\\d+$")) {
                    //是门钥匙
                    r_locs.computeIfAbsent(name, _ -> new ArrayList<>())
                            .add(location);
                } else {
                    //todo 其他物品随机
                }
            });
            final_names.putIfAbsent(location, names);
        }
        r_locs.forEach((name, locs) -> {
            //随机一个尸体
            final_names.get(locs.get(random.nextInt(locs.size()))).add(name);
        });
        final_names.forEach(this::spawnPickableBody);
    }

    /**
     * 生成尸体
     * @param location 位置
     * @param item_names 物品名列表
     */
    public void spawnPickableBody(Location location, List<String> item_names) {
        pickable_bodies.put(location.getWorld().spawn(location.clone().add(0, -1.5, 0), ArmorStand.class, ar -> {
            ar.setGravity(false);
            ar.setVisible(false);
            ar.setInvulnerable(true);
            ar.setDisabledSlots(EquipmentSlot.values());
            ar.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
            //todo
        }), item_names);
    }

    private void spawnBarriers() {
        //板子
        var barriersInfo = (List<String>) locations_config.getList("barriers");
        for (String barrierInfo : barriersInfo) {
            var info = barrierInfo.split(" ");
            var xyz = Arrays.stream(info[0].split(",")).mapToDouble(Double::parseDouble).toArray();
            Location location = new Location(world, xyz[0], xyz[1], xyz[2]);
            barriers.put(location, new Barrier(location, info[1].equals("N") ? BlockFace.EAST : BlockFace.NORTH));
        }
    }

    public static boolean isRunning() {
        return isStarted() && !game.isPaused;
    }

    public static boolean isStarted() {
        return game != null && game.chooseRole == null;
    }

    public void start() {
        start(false);
    }

    /**
     * 暂停游戏
     */
    public void pause() {
        isPaused = true;
        playerData.keySet().forEach(player -> player.sendMessage(rMsg("<dark_red>游戏暂停，��离开玩家未返回将在1分钟后自动结束")));
        pause_task = new BukkitRunnable() {
            @Override
            public void run() {
                end(null);
            }
        }.runTaskLater(plugin, 1200);
    }

    /**
     * 结束游戏
     * @param ender 输入/reset的玩家
     */
    public void end(@Nullable Player ender) {
        destroy();
        game = null;
        playerData.keySet().forEach(Game::resetPlayer);
        Bukkit.getScheduler().cancelTasks(plugin);
        new TickRunner().runTaskTimer(plugin, 0, 1);
        var world = Bukkit.getWorld("world");
        assert world != null;
        if (ender != null) world.sendMessage(ender.name().append(rMsg("强制结束了游戏")));
        else world.sendMessage(rMsg("游戏结束,%s胜利".formatted(survivorScore > hunterScore ? "幸存者" : "猎人")));
    }

    public static void resetPlayer(Player player) {
        Map.of(
                MOVEMENT_SPEED, .1,
                ATTACK_DAMAGE, 1.0,
                MAX_ABSORPTION, 20.0,
                ATTACK_SPEED, 255.0,
                ATTACK_KNOCKBACK, -1.0,
                JUMP_STRENGTH, .0,
                ENTITY_INTERACTION_RANGE, 4.0
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
                    .modelData(20000)
                    .name(Component.text("主菜单", NamedTextColor.GOLD))
                    .build()
            );
            player.setGameMode(GameMode.ADVENTURE);
        } else {
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    /**
     * 结束时的清理
     */
    public void destroy() {
        if (chooseRole != null) {
            chooseRole.roles.keySet().forEach(Entity::remove);
        }
        signal_repeaters.keySet().forEach(Entity::remove);
        item_on_ground.keySet().forEach(Entity::remove);
        pickable_bodies.keySet().forEach(Entity::remove);
        for (BlockDisplay blockDisplay : lifts.keySet()) {
            LocationFactory.replace2x2Lift(blockDisplay.getLocation(), Material.AIR, BlockFace.SELF);
            blockDisplay.remove();
        }
        markers.forEach(Entity::remove);
        TickRunner.tickRunnable.clear();
        TickRunner.effectList.clear();
    }

    public void spawnPlayerBody(Player player) {
        markers.add(player.getWorld().spawn(player.getLocation().add(0, -1.5, 0), ArmorStand.class, armorStand -> {
            armorStand.setInvisible(true);
            armorStand.setMarker(true);
            armorStand.getEquipment().setHelmet(new ItemStack(Material.PLAYER_HEAD) {{
                editMeta(meta -> ((SkullMeta) meta).setOwningPlayer(player));
            }});
            armorStand.addScoreboardTag("body");
            armorStand.setDisabledSlots(EquipmentSlot.values());
        }));
    }

    public void intoSecondStage() {
        Bukkit.getScheduler().runTaskLater(plugin, _ -> {
            ((Hunter) playerData.get(hunter).getRole()).strengthen();
            Portal.summon();
        }, 20 * 60);
    }

    /**
     * 幸存者通过传送门逃出
     * @param player 逃出的人
     */
    public void survivorRunOut(Player player) {
        survivorScore += 6;
        Game.resetPlayer(player);
        player.setGameMode(GameMode.SPECTATOR);
        Bukkit.getAsyncScheduler().runNow(plugin, _ -> {
            game.survivors.remove(player);
            if (game.survivors.isEmpty()) {
                TickRunner.gameEnd = true;
            }
        });
    }

    /**
     * 封印幸存者
     * @param player 被封印者
     * @param dom 鬼界
     */
    public void sealSurvivor(Player player, GhostDom dom) {
        var pd = PlayerData.of(player);
        Survivor survivor = (Survivor) pd.getRole();
        if (survivor.isSealed()) return;
        hunterScore++;
        if (survivor.seal(dom)) hunterScore += 6;
    }
}