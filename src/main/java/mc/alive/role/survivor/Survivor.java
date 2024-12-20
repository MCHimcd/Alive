package mc.alive.role.survivor;

import mc.alive.Game;
import mc.alive.PlayerData;
import mc.alive.StoredData;
import mc.alive.effect.Giddy;
import mc.alive.item.DoorCard;
import mc.alive.item.GameItem;
import mc.alive.mechanism.GhostDom;
import mc.alive.role.Role;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Random;

import static mc.alive.Game.game;


abstract public class Survivor extends Role {
    protected final int feature = StoredData.playerStoredData.get(player).getOption(StoredData.Option.FEATURE);
    public int heartbeat_tick = 0;    //心跳cd
    protected boolean hurt = false;
    protected boolean down = false;
    protected boolean sealed = false;
    protected boolean captured = false;
    private int pickup_body_tick = 0;   //捡尸体计时
    private int seal_count = 0; //被封印次数

    protected Survivor(Player p, int id) {
        super(p, id);
    }

    public boolean isSealed() {
        return sealed;
    }

    public int getFeature() {
        return feature;
    }

    /**
     * @return 维修速度
     */
    abstract public int getFixSpeed();

    public boolean isDown() {
        return down;
    }

    public void setCaptured(boolean captured) {
        this.captured = captured;
    }

    public void heartbeatTick() {
        if (heartbeat_tick > 0) heartbeat_tick--;
    }

    public void damage() {
        if (down) sealed = true;
        if (!hurt) hurt = true;
        else down = true;
    }

    public void heal() {
        if (down) down = false;
        else if (hurt) hurt = false;
        if (sealed) sealed = false;
    }

    public boolean seal(GhostDom dom) {
        if (++seal_count == 3) {
            PlayerData.of(player).die();
            return true;
        }
        dom.addPlayer(player);
        sealed = true;
        return false;
    }

    @Override
    public void tick() {
        if (!Game.isRunning()) return;
        //捡尸体
        if (player.isSneaking() && !down) {
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
        if (!captured && !sealed && heartbeat_tick <= 0) {
            player.playSound(player, Sound.BLOCK_ANVIL_LAND, 1, 1);
            player.spawnParticle(Particle.DUST, player.getEyeLocation(), 10, 0.1, 0.2, 0.1, new Particle.DustOptions(Color.RED, 1));
            heartbeat_tick = 40;
        }
        if (hurt && !sealed && !down) {
            player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation(), 10, 0.1, 0, 0.1, Bukkit.createBlockData(Material.REDSTONE_BLOCK));
        }
        if (down && !sealed) {
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation(), 10, 0.1, 0, 0.1, new Particle.DustOptions(Color.RED, 1));
            getPlayerData().addEffect(new Giddy(player, 2));
        }
        if (down || captured || sealed) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 100, true, false));
        }
    }
}
