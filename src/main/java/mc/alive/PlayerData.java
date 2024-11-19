package mc.alive;

import mc.alive.effect.Effect;
import mc.alive.effect.Giddy;
import mc.alive.effect.MultilevelEffect;
import mc.alive.effect.Speed;
import mc.alive.item.DoorCard;
import mc.alive.item.GameItem;
import mc.alive.role.Role;
import mc.alive.role.Skill;
import mc.alive.role.hunter.Hunter;
import mc.alive.role.survivor.Survivor;
import mc.alive.tick.TickRunnable;
import mc.alive.tick.TickRunner;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static mc.alive.Alive.plugin;
import static mc.alive.Game.game;
import static mc.alive.tick.PlayerTickrunnable.chosen_duct;
import static mc.alive.tick.PlayerTickrunnable.chosen_item_display;
import static mc.alive.util.Message.rMsg;

public final class PlayerData implements TickRunnable {
    private final List<Effect> effects = new ArrayList<>();
    private final Role role;
    private final List<Integer> skill_cd = new ArrayList<>(Arrays.asList(0, 0, 0, 0));
    private final Map<Integer, Method> skill_methods = new HashMap<>();
    //维修cd
    public int fix_tick = -1;
    //攻击cd
    public double attack_cd = -1;
    //心跳cd
    public int heartbeat_tick = 0;
    //hunter破坏机子cd
    public int break_tick = 0;
    private Player player;
    //选择的技能
    private int current_skill_id = 0;
    //选择技能重置cd
    private int current_skill_reset_cd = 0;
    private double health;
    //hunter回血间隔
    private int health_tick = 0;
    //捡尸体计时
    private int pickup_body_tick = 0;

    public PlayerData(Player player, Role role) {
        this.player = player;
        this.role = role;
        if (role instanceof Survivor) {
            Game.team_survivor.addPlayer(player);
        } else if (role instanceof Hunter h) {
            Game.team_hunter.addPlayer(player);
            damageOrHeal(-h.getMaxHealth());
        }

        Arrays.stream(role.getClass().getMethods()).forEach(m -> {
            var a = m.getAnnotation(Skill.class);
            if (a != null) {
                skill_methods.put(a.id(), m);
            }
        });
        for (int i = 0; i < role.getSkillCount(); i++) {
            skill_cd.add(0);
        }

        @SuppressWarnings("DataFlowIssue") var name = rMsg(((String) Alive.roles_config.get(String.valueOf(role.getRoleID()))).split(" ")[1]);
        player.displayName(name);
        player.playerListName(name);

        startTick();
    }

    /**
     * @param amount 正为扣血，负为加血
     */
    public void damageOrHeal(double amount) {
        if (amount > 0) {
            player.damage(0.01);
            BlockData blood = Material.REDSTONE_BLOCK.createBlockData();
            player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation().clone().add(0, 1, 0), 100, 0.3, 0.3, 0.3, blood);

        }
        if (role instanceof Survivor s) {
            if (amount > 0) {
                //伤害
                if (!s.isHurt()) {
                    s.setHurt(true);
                } else {
                    s.setDown(true);
                }
            } else {
                //治疗
                if (s.isDown()) {
                    s.setDown(false);
                } else if (s.isHurt()) {
                    s.setHurt(false);
                }
            }
            //加速
            addEffect(new Speed(player, 20, 0));
        } else if (role instanceof Hunter h) {
            if (amount >= 0) {
                //减血量
                if (health >= 0) {
                    health -= amount;
                    //回血时间=剩余血量%*200
                    health_tick = (int) Math.max(100, (Math.max(0, health / h.getMaxHealth()) * 400));
                    player.damage(0.01);
                }
                health = Math.max(-5, health);
            } else health = Math.min(health - amount, h.getMaxHealth());
            if (health < 0) player.setHealth(0.1);
            else player.setHealth(Math.max(0.1, 20 * health / h.getMaxHealth()));
            //减速
            AttributeInstance speed = requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED));
            if (health <= h.getMaxHealth() * 0.5) {
                speed.setBaseValue(role.getSpeed() * 0.3);
            } else if (health <= h.getMaxHealth() * 0.2) {
                speed.setBaseValue(-1);
            } else speed.setBaseValue(h.getSpeed());
        }
    }

    public void addEffect(Effect effect) {
        var e = effects.stream().filter(e1 -> e1.getClass().equals(effect.getClass())).findFirst();
        if (e.isPresent()) {
            if (!(e.get() instanceof MultilevelEffect)) {
                e.get().addTime(effect.getTime());
            } else {
                effects.add(effect);
            }
        } else {
            effects.add(effect);
        }
    }

    /**
     * 设置Role的技能的CD
     * @param player 玩家
     * @param index 技能id，从1开始
     * @param amount 单位为tick
     */
    public static void setSkillCD(Player player, int index, int amount) {
        assert game != null;
        game.playerData.get(player).skill_cd.set(index, amount);
    }

    private void die() {
        player.getWorld().sendMessage(player.displayName().append(rMsg("%s".formatted(List.of(
                "去世了",
                "离开了人间."
        ).get(new Random().nextInt(0, 2))))));
        game.spawnPlayerBody(player);
        Game.resetPlayer(player);
        player.setGameMode(GameMode.SPECTATOR);
        Bukkit.getAsyncScheduler().runNow(plugin, _ -> {
            game.survivors.remove(player);
            if (game.survivors.isEmpty()) {
                TickRunner.gameEnd = true;
            }
        });
    }

    public void setPlayer(Player player) {
        role.setPlayer(player);
        this.player = player;
    }

    public Role getRole() {
        return role;
    }

    public boolean hasEffect(Class<? extends Effect> effect) {
        return effects.stream().anyMatch(e -> e.getClass().equals(effect));
    }

    public void removeEffect(Class<? extends Effect> effect) {
        effects.stream().filter(effect::isInstance).findFirst().ifPresent(effect1 -> effect1.addTime(-1000000000));
    }

    @Override
    public void tick() {
        if (game.isPaused) return;
        //Effect
        effects.stream()
                .filter(effect -> effect instanceof MultilevelEffect)
                .map(e -> (MultilevelEffect) e)
                .collect(Collectors.groupingBy(
                        Object::getClass,
                        Collectors.maxBy(Comparator.comparingInt(MultilevelEffect::getLevel))
                ))
                .forEach((_, effect) -> effect.ifPresent(effect1 -> {
                    if (effect1.shouldRemove()) effects.remove(effect1);
                }));
        effects.removeIf(effect -> !(effect instanceof MultilevelEffect) && effect.shouldRemove());

        //心跳和受伤粒子
        if (Game.isRunning()) {
            if (role instanceof Survivor s) {
                if (!s.isCaptured() && heartbeat_tick <= 0) {
                    player.playSound(player, Sound.BLOCK_ANVIL_LAND, 1, 1);
                    player.spawnParticle(Particle.DUST, player.getEyeLocation(), 10, 0.1, 0.2, 0.1, new Particle.DustOptions(Color.RED, 1));
                    heartbeat_tick = 40;
                }
                if (s.isHurt()) {
                    player.spawnParticle(Particle.BLOCK, player.getLocation(), 10, 0.1, 0, 0.1, Bukkit.createBlockData(Material.REDSTONE_BLOCK));
                }
                if (s.isDown()) {
                    player.spawnParticle(Particle.DUST, player.getLocation(), 10, 0.1, 0, 0.1, new Particle.DustOptions(Color.RED, 1));
                }
            } else if (role instanceof Hunter hunter) {
                player.getWorld().getNearbyPlayers(player.getLocation(), hunter.getPursuitFeature() == 0 ? 12 : 18, p -> !p.equals(player))
                        .forEach(pl -> --PlayerData.of(pl).heartbeat_tick);
            }
        }

        //捡尸体
        if (role instanceof Survivor && player.isSneaking()) {
            var body = game.pickable_bodies.keySet().stream()
                    .filter(entity -> player.getWorld().getNearbyPlayers(entity.getLocation().add(0, 1.5, 0), 1).contains(player))
                    .findFirst().orElse(null);
            if (body != null && ++pickup_body_tick == 60) {
                Random r = new Random();
                game.pickable_bodies.get(body).forEach(name -> {
                    int key_id = 0, count = 1;
                    var c_index = name.indexOf("*");
                    if (c_index != -1) {
                        count = Integer.parseInt(name.substring(c_index + 1));
                        name = name.substring(0, c_index);
                    }
                    if (name.startsWith("DoorCard")) {
                        key_id = Integer.parseInt(name.substring(8));
                        name = "DoorCard";
                    }
                    var clazz = GameItem.registries.get(name);
                    if (clazz != null) {
                        var it = game.spawnItem(clazz, player.getLocation(), count, clazz.equals(DoorCard.class) ? key_id : null);
                        it.setVelocity(new Vector(
                                (r.nextBoolean() ? 1 : -1) * r.nextDouble() * 0.3,
                                0.2,
                                (r.nextBoolean() ? 1 : -1) * r.nextDouble() * 0.3
                        ));
                    }
                });
                game.pickable_bodies.remove(body);
                body.remove();
            }
        } else pickup_body_tick = 0;

        //普攻冷却
        attack_cd = Math.max(0, attack_cd - 0.05);
        if (attack_cd > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 1, 100, true, false));
        }

        //血量恢复
        if (health_tick > 0) health_tick--;
        if (role instanceof Hunter && health_tick == 0) {
            damageOrHeal(-0.25);
        }

        //维修或破坏
        var target = chosen_item_display.get(player);
        if (fix_tick >= 0) {
            if (--fix_tick == 0 && target != null) {
                player.getLocation().getNearbyPlayers(20).forEach(player1 ->
                        player1.playSound(player1, Sound.ENTITY_IRON_GOLEM_REPAIR, 1f, 1f));
                if (role instanceof Survivor survivor) {
                    game.fixGenerator(target, survivor.getFixSpeed());
                } else if (role instanceof Hunter hunter && break_tick-- <= 0) {
                    break_tick = 90 * 20;
                    game.breakGenerator(target, hunter.getOtherFeature() == 0 ? 0.8 : 0.9);
                }
            }
            if (role instanceof Hunter) {
                addEffect(new Giddy(player, 1));
            }
        }

        //技能冷却
        for (int i = 0; i < skill_cd.size(); i++) {
            if (skill_cd.get(i) > 0) skill_cd.set(i, skill_cd.get(i) - 1);
        }

        //技能选择
        current_skill_reset_cd = Math.max(0, current_skill_reset_cd - 1);
        if (current_skill_reset_cd == 0) {
            current_skill_id = 0;
        }
    }

    /**
     * @return 玩家对应的PlayerData，若游戏未开始则返回null
     */
    public static PlayerData of(Player player) {
        return game.playerData.get(player);
    }

    /**
     * 将选择的技能改为下一个
     */
    public void changeSkillValue() {
        //蓄力技能取消
        role.removeSkillLocation(Role.ZERO_LOC);

        ++current_skill_id;
        skill_methods.forEach((id, m) -> {
            if (id == current_skill_id) {
                var a = m.getAnnotation(Skill.class);
                if (role instanceof Hunter hunter && a.minLevel() > hunter.getLevel()) {
                    ++current_skill_id;
                    return;
                }
                //找到
                current_skill_reset_cd = 20;
                if (skill_cd.get(current_skill_id) == 0) {
                    player.showTitle(
                            Title.title(
                                    Component.text("§a·".repeat(id)),
                                    Component.text(a.name(), TextColor.color(0, 255, 255)),
                                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
                            )
                    );
                } else {
                    player.showTitle(
                            Title.title(
                                    Component.text("§a·".repeat(id)),
                                    Component.text("冷却中", NamedTextColor.DARK_RED),
                                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
                            )
                    );
                }
                player.playSound(player, Sound.UI_BUTTON_CLICK, .3f, 10f);
            }
        });
        if (current_skill_id > role.getSkillCount()) {
            current_skill_id = 0;
            clearTitle();
        }
    }

    private void clearTitle() {
        player.showTitle(
                Title.title(
                        Component.empty(),
                        Component.empty()
                )
        );
    }

    /**
     * 尝试使用选中的技能
     */
    public void useSkill() {
        skill_methods.forEach((id, m) -> {
            if (id == current_skill_id && skill_cd.get(id) == 0) {
                try {
                    current_skill_id = 0;
                    skill_cd.set(0, 1);
                    m.invoke(role);
                    clearTitle();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * 尝试进入管道
     */
    public void tryIntoDuct() {
        if (chosen_duct != null) player.teleport(chosen_duct.clone().setDirection(player.getLocation().getDirection()));
    }

    /**
     * 若是Hunter则升级
     */
    public void tryLevelUp() {
        if (!(role instanceof Hunter h)) return;
        h.levelUp();
    }

    public boolean canMove() {
        if (role instanceof Hunter h) {
            return health > h.getMaxHealth() * 0.2;
        }
        return true;
    }

}