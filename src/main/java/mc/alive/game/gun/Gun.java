package mc.alive.game.gun;

import io.papermc.paper.entity.LookAnchor;
import mc.alive.Alive;
import mc.alive.game.PlayerData;
import mc.alive.util.Factory;
import mc.alive.util.ItemBuilder;
import mc.alive.util.Message;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static mc.alive.Alive.game;

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
    protected int count = 0;
    protected boolean canShoot = true;
    protected boolean reloading = false;
    private BukkitTask reload_task;

    //枪的数值 和 模型id
    //后坐力  子弹类型  伤害  最大容量 穿透力

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

        if (findBullet(player, bulletType, capacity - count, false) == 0) return;

        reload_task = new BukkitRunnable() {
            int reloadtime = 0;

            @Override
            public void run() {
                Component progressBar = Message.rMsg(" 子弹装填中: " + "<aqua>" + "|".repeat(reloadtime) + "<white>" + "|".repeat(reload_time - reloadtime));
                player.sendActionBar(progressBar);
                if (reloadtime % 10 == 0) {
                    player.playSound(player, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, .5f, .5f);
                }
                if (reloadtime++ >= reload_time) {
                    count += findBullet(player, bulletType, capacity - count, true);
                    player.sendMessage(Component.text(count));
                    reloading = false;
                    cancel();
                }
            }
        }.runTaskTimer(Alive.plugin, 0, 1);
    }

    //射击
    public void shoot(Player player) {
        if (count == 0) {
            player.sendActionBar(Component.text("null"));
            return;
        }
        if (!canShoot) return;

        //间隔
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                canShoot = true;
                timer.cancel();
            }
        }, shoot_interval);
        canShoot = false;

        count--;
        player.sendActionBar(Component.text("%s / %s".formatted(count, capacity)));
        player.playSound(player, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1f, 1f);

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

        //后坐力
        var l = player.getEyeLocation().add(player.getEyeLocation().getDirection().add(new Vector(0, reactiveForce, 0)));
        player.lookAt(l.getX(), l.getY(), l.getZ(), LookAnchor.EYES);
//        Factory.setYawPitch(player.getYaw(), player.getPitch() - reactiveForce, player);
    }

    public enum BulletType {
        Chamber_Standard_Cartridge(90000);

        private final int data;

        private BulletType(int data) {
            this.data = data;
        }

        public int getData() {
            return data;
        }
    }

    public static ItemStack getGunItemStack(int data) {
        return switch (data) {
            case 80000 -> ItemBuilder.material(Material.DIAMOND_HOE, data).build();
            case 80001 -> ItemBuilder.material(Material.GOLDEN_AXE, data).build();
            default -> throw new IllegalStateException("Unexpected value: " + data);
        };
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

    public void changeItem() {
        if (reloading) {
            reloading = false;
            reload_task.cancel();
        }
    }
}
