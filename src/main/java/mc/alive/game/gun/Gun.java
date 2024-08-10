package mc.alive.game.gun;

import mc.alive.Alive;
import mc.alive.game.PlayerData;
import mc.alive.util.Factory;
import mc.alive.util.ItemBuilder;
import mc.alive.util.Message;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static mc.alive.Alive.game;
import static mc.alive.Alive.plugin;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

public abstract class Gun {

    protected final float reactiveForce;
    private final BulletType bulletType;
    protected final double damage;
    protected final int capacity;
    //mSec
    protected final long shoot_interval;
    private final ItemStack item;
    //tick
    private final int reload_time;
    protected int remained_bullet = 0;
    protected boolean canShoot = true;
    protected boolean reloading = false;
    private BukkitTask reload_task;
    private Timer timer = new Timer();

    //枪的数值 和 模型id
    //后坐力  子弹类型  伤害  最大容量 穿透力

    public void startShoot(Player player) {
        if (canShoot) {
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
                    if (c[0]) cancel();
                }
            }, 0, shoot_interval);
            setCanShoot(player, false);
        }
    }

    public void stopShoot(@Nullable Player player) {
        timer.cancel();
        if (player == null) return;
        setCanShoot(player, false);
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (remained_bullet > 0) setCanShoot(player, true);
            }
        }, shoot_interval);
        player.getInventory().setItem(35, null);

    }

    protected Gun(ItemStack item, float reactiveForce, BulletType bulletType, double damage, int capacity, long shoot_interval, int reload_time) {
        this.item = item;
        this.reactiveForce = reactiveForce;
        this.bulletType = bulletType;
        this.damage = damage;
        this.capacity = capacity;
        this.shoot_interval = shoot_interval;
        this.reload_time = reload_time;
    }

    private static int findBullet(Player player, BulletType bulletType, int count, boolean remove) {
        int finalCount = 0;
        var contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            var it = player.getInventory().getItem(i);
            if (it == null || !it.hasItemMeta() || !it.getItemMeta().hasCustomModelData()) continue;
            var data = it.getItemMeta().getCustomModelData();
            if (data != bulletType.getData()) continue;
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

    //重新装填
    public void reload(Player player) {
        if (reloading) return;
        reloading = true;

        if (findBullet(player, bulletType, capacity - remained_bullet, false) == 0) return;

        reload_task = new BukkitRunnable() {
            int re_time = 0;

            @Override
            public void run() {
                Component progressBar = Message.rMsg(" 子弹装填中: " + "<aqua>" + "|".repeat(re_time) + "<white>" + "|".repeat(reload_time - re_time));
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

    //射击
    protected boolean shoot(Player player) {
        if (cannotShoot(player)) return false;

        var pd = PlayerData.getPlayerData(player);
        var result = shootPath(player);

        //粒子
        for (Location location : result.path()) {
            player.getWorld().spawnParticle(Particle.DUST, location, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.ORANGE, 1f), true);
        }

        //伤害
        if (result.target() != null) {
            PlayerData.getPlayerData(result.target()).damageOrHeal(damage);
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

    public enum BulletType {
        Chamber_Standard_Cartridge(90001);

        private final int data;

        BulletType(int data) {
            this.data = data;
        }

        public int getData() {
            return data;
        }
    }

    public static ItemStack getGunItemStack(int data) {
        return ItemBuilder.material(Material.BOW, data).name(switch (data) {
            case 80000 -> text("手枪");
            case 80001 -> text("霰弹枪");
            case 80002 -> text("冲锋枪");
            default -> empty();
        }).build();
    }

    //射击路径
    protected Result shootPath(Player player) {
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
                return new Result(Factory.line(player.getEyeLocation().subtract(0, 1, 0), position.toLocation(player.getWorld()).subtract(0, 1, 0), 0.5), (Player) target);
            }
        }
        var end = player.getEyeLocation().add(player.getLocation().getDirection().normalize().multiply(100));
        return new Result(Factory.line(player.getEyeLocation().subtract(0, 1, 0), end, 0.5));
    }

    public record Result(List<Location> path, Player target) {
        public Result(List<Location> path) {
            this(path, null);
        }
    }

    public void handleItemChange(Player player, boolean previous) {
        if (previous) {
            //切换之前的
            if (reloading) {
                reloading = false;
                reload_task.cancel();
            }
            timer.cancel();
            player.getInventory().setItem(35, new ItemStack(Material.AIR));
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

    @SuppressWarnings("DataFlowIssue")
    private void setCanShoot(Player player, boolean value) {
        canShoot = value;
        if (canShoot) {
            player.getInventory().setItem(35, ItemBuilder.material(Material.ARROW, 90000).build());
            player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(255);
        } else {
            player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(-1);
        }
    }

}
