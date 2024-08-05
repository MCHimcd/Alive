package mc.alive.game.gun;

import mc.alive.Alive;
import mc.alive.game.PlayerData;
import mc.alive.game.role.survivor.Survivor;
import mc.alive.util.Factory;
import mc.alive.util.ItemBuilder;
import mc.alive.util.Message;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Timer;
import java.util.TimerTask;

public abstract class Gun {

    private final float reactiveForce;
    private final BulletType bulletType;
    private final double damage;
    private final int capacity;
    private final long shoot_interval;
    private final ItemStack item;
    //
    private final int reload_time;
    private int count = 0;
    private boolean canShoot = true;
    private boolean reloading = false;

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

    private static int findBullet(Player player, BulletType bulletType, int count) {
        int finalCount = 0;
        var contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            var it = player.getInventory().getItem(i);
            if (it == null || !it.hasItemMeta() || !it.getItemMeta().hasCustomModelData()) continue;
            var data = it.getItemMeta().getCustomModelData();
            if (data != bulletType.getData()) continue;
            if (it.getAmount() > count) {
                finalCount += count;
                it.setAmount(it.getAmount() - count);
                return finalCount;
            } else {
                count -= it.getAmount();
                finalCount += it.getAmount();
                player.getInventory().setItem(i, null);
            }
        }
        player.updateInventory();
        return finalCount;
    }

    //重新装填
    public void reload(Player player) {
        if (reloading) return;
        reloading = true;
        new BukkitRunnable() {
            int reloadtime = 0;

            @Override
            public void run() {
                Component progressBar = Message.rMsg(" 子弹装填中: " + "<aqua>" + "|".repeat(reloadtime) + "<white>" + "|".repeat(reload_time - reloadtime));
                player.sendActionBar(progressBar);
                player.playSound(player, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, .5f, .5f);
                if (reloadtime++ >= reload_time) {
                    count += findBullet(player, bulletType, capacity - count);
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
        if (!(pd.getRole() instanceof Survivor survivor)) return;
        var result = survivor.shootPath();

        //粒子
        for (Location location : result.path()) {
            player.getWorld().spawnParticle(Particle.DUST, location, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.ORANGE, 1f), true);
        }

        //伤害
        if (result.target() != null) {
            PlayerData.getPlayerData(result.target()).damageOrHeal(damage);
        }

        //后坐力
        Factory.setYawPitch(player.getYaw(), player.getPitch() - reactiveForce, player);
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
            default -> throw new IllegalStateException("Unexpected value: " + data);
        };
    }
}
