package mc.alive.item.usable.gun;

import mc.alive.Alive;
import mc.alive.PlayerData;
import mc.alive.item.GameItem;
import mc.alive.item.PickUp;
import mc.alive.item.usable.Usable;
import mc.alive.role.survivor.Survivor;
import mc.alive.util.LocationFactory;
import mc.alive.util.Message;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static mc.alive.Alive.plugin;
import static mc.alive.Game.game;
import static mc.alive.PlayerData.of;
import static net.kyori.adventure.text.Component.text;

public abstract class Gun extends GameItem implements Usable {

    protected final float reactiveForce;
    protected final double damage;
    protected final int capacity;
    protected final long shoot_interval;
    private final Class<? extends GameItem> bulletType;
    private final int reload_time;
    protected int remained_bullet = 0;
    protected boolean canShoot = true;
    protected boolean reloading = false;
    private BukkitTask reload_task;
    private Timer timer = new Timer();
    private long timer_next_run = 0;

    /**
     * @param damage         伤害
     * @param capacity       子弹容量
     * @param reload_time    装弹时间(tick)
     * @param reactiveForce  后坐力
     * @param shoot_interval 射击间隔(ms)
     * @param bulletType     子弹类型
     */
    protected Gun(double damage, int capacity, int reload_time, float reactiveForce, long shoot_interval, Class<? extends GameItem> bulletType) {
        this.reactiveForce = reactiveForce;
        this.bulletType = bulletType;
        this.damage = damage;
        this.capacity = capacity;
        this.shoot_interval = shoot_interval;
        this.reload_time = reload_time;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (timer_next_run < shoot_interval) {
                    timer_next_run++;
                }
            }
        }, 0, 1);
    }

    @Override
    public Material material() {
        return Material.HONEY_BOTTLE;
    }

    @Override
    public PickUp pickUp() {
        return PickUp.SURVIVOR;
    }

    @Override
    public void handleItemUse(Player player) {
        if (!(of(player).getRole() instanceof Survivor)) return;
        if (canShoot) {
            if (reloading) {
                reloading = false;
                reload_task.cancel();
            }
            timer.cancel();
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    final boolean[] c = {false};
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            c[0] = !shoot(player);
                        }
                    }.runTask(plugin);
                    if (c[0]) {
                        cancel();
                    } else {
                        timer_next_run = 0;
                    }
                }
            }, 0, shoot_interval);
            setCanShoot(player, false);
        }
    }

    //射击
    protected boolean shoot(Player player) {
        if (cannotShoot(player)) return false;

        var result = shootPath(player);

        //粒子
        for (Location location : result.path()) {
            player.getWorld().spawnParticle(Particle.DUST, location, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.ORANGE, 1f), true);
        }

        //伤害
        if (result.target() != null) {
            PlayerData.of(result.target()).damageOrHeal(damage);
        }

        applyRecoil(player);
        return true;
    }

    protected void applyRecoil(Player player) {
        float pitch = player.getEyeLocation().getPitch();
        float yaw = player.getEyeLocation().getYaw();

        player.setRotation(yaw, pitch - reactiveForce);
    }

    protected boolean cannotShoot(Player player) {
        if (remained_bullet == 0) {
            player.sendActionBar(text("null"));
            stopShoot(player);
            return true;
        }

        remained_bullet--;
        player.sendActionBar(text("%s / %s".formatted(remained_bullet, capacity)));
        player.playSound(player, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1f, 1f);
        return false;
    }

    public void stopShoot(@Nullable Player player) {
        if (player == null) return;
        timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (remained_bullet > 0) {
                    setCanShoot(player, true);
                }
            }
        }, shoot_interval - timer_next_run);
        setCanShoot(player, false);
    }

    @SuppressWarnings("DataFlowIssue")
    private void setCanShoot(Player player, boolean value) {
        canShoot = value;
        if (canShoot) {
            player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(255);
        } else {
            player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(-1);
        }
    }

    /**
     * @return 射击路径（Location集合和射中的目标）
     */
    protected ResultPath shootPath(Player player) {
        var result = player.getWorld().rayTrace(
                player.getEyeLocation(),
                player.getLocation().getDirection(),
                100,
                FluidCollisionMode.NEVER,
                true,
                0.1,
                entity -> entity instanceof Player p && p.equals(game.hunter)
        );
        if (result != null) {
            var target = result.getHitEntity();
            if (target != null) {
                var position = result.getHitPosition();
                return new ResultPath(LocationFactory.line(player.getEyeLocation().subtract(0, 1, 0), position.toLocation(player.getWorld()).subtract(0, 1, 0), 0.5), (Player) target);
            }
            var block = result.getHitBlock();
            if (block != null) {
                var position = result.getHitPosition();
                return new ResultPath(LocationFactory.line(player.getEyeLocation().subtract(0, 1, 0), position.toLocation(player.getWorld()).subtract(0, 1, 0), 0.5), (Player) target);
            }
        }
        var end = player.getEyeLocation().add(player.getLocation().getDirection().normalize().multiply(100));
        return new ResultPath(LocationFactory.line(player.getEyeLocation().subtract(0, 1, 0), end, 0.5));
    }

    public void reload(Player player) {
        if (reloading) return;
        reloading = true;

        if (findBullet(player, bulletType, capacity - remained_bullet, false) == 0) return;
        timer.cancel();

        reload_task = new BukkitRunnable() {
            int re_time = 0;

            @Override
            public void run() {
                Component progressBar = player.getInventory().getItemInMainHand().getItemMeta().itemName()
                        .append(Message.rMsg(" 子弹装填中: " + "<aqua>" + "|".repeat(re_time) + "<white>" + "|".repeat(reload_time - re_time)));
                player.sendActionBar(progressBar);
                if (re_time % 10 == 0) {
                    player.playSound(player, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, .5f, .5f);
                }
                if (re_time++ >= reload_time) {
                    remained_bullet += findBullet(player, bulletType, capacity - remained_bullet, true);
                    reloading = false;
                    setCanShoot(player, true);
                    cancel();
                }
            }
        }.runTaskTimer(Alive.plugin, 0, 1);
    }

    /**
     * @param count  期望数量
     * @param remove 是否清除找到的物品
     * @return 最终找到的数量
     */
    private static int findBullet(Player player, Class<? extends GameItem> bulletType, int count, boolean remove) {
        int finalCount = 0;
        var contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            var it = player.getInventory().getItem(i);
            if (it == null || !it.hasItemMeta() || !it.getItemMeta().hasCustomModelData()) continue;
            var data = it.getItemMeta().getCustomModelData();
            try {
                if (data != bulletType.getDeclaredConstructor().newInstance().customModelData()) continue;
            } catch (Exception e) {
                plugin.getLogger().warning(e.getLocalizedMessage());
            }
            if (it.getAmount() > count) {
                finalCount += count;
                if (remove) it.setAmount(it.getAmount() - count);
                return finalCount;
            } else {
                count -= it.getAmount();
                finalCount += it.getAmount();
                if (remove) player.getInventory().setItem(i, null);
            }
        }
        player.updateInventory();
        return finalCount;
    }

    /**
     * 玩家切换快捷栏选中的物品时触发
     *
     * @param previous 该物品是否为玩家之前选中的物品
     */
    public void handleItemChange(Player player, boolean previous) {
        if (previous) {
            //切换之前的
            if (reloading) {
                reloading = false;
                reload_task.cancel();
            }
            timer.cancel();
        } else {
            //切换之后的
            setCanShoot(player, false);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (remained_bullet > 0) setCanShoot(player, true);
                }
            }.runTaskLater(plugin, 20);
        }
    }

    public record ResultPath(List<Location> path, Player target) {
        public ResultPath(List<Location> path) {
            this(path, null);
        }
    }

}
