package mc.alive.mechanism;

import mc.alive.Game;
import mc.alive.util.LocationFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class LiftDoor {
    private final Block block;
    private final Lift lift;
    private final int floor;
    private final BlockDisplay blockDisplay_right;
    private final BlockDisplay blockDisplay_left;
    private final Vector3f move_dir_right;
    private final Vector3f move_dir_left;

    public LiftDoor(Block block, Lift lift, int floor) {
        lift.liftDoors.add(this);
        this.lift = lift;
        this.block = block;
        this.floor = floor;

        var direction = getFace().getDirection();
        var dir = new Vector3f((float) direction.getX(), (float) direction.getY(), (float) direction.getZ());
        //右边门移动方向
        move_dir_right = new Vector3f(-dir.z, dir.y, dir.x);
        //左边门移动方向
        move_dir_left = new Vector3f(-move_dir_right.x, move_dir_right.y, -move_dir_right.z);

        var q = new Quaternionf();
        q.lookAlong(dir, new Vector3f(0, 1, 0));

        blockDisplay_right = block.getWorld().spawn(block.getLocation(), BlockDisplay.class, bd -> {
            bd.setTransformation(new Transformation(
                    LocationFactory.getTranslation(getFace()),
                    q,
                    new Vector3f(2, 2, 1),
                    new Quaternionf()
            ));
            bd.setBlock(Bukkit.createBlockData(Material.IRON_BLOCK));
        });
        Game.game.markers.add(blockDisplay_right);

        blockDisplay_left = block.getWorld().spawn(block.getLocation(), BlockDisplay.class, bd -> {
            bd.setTransformation(new Transformation(
                    new Vector3f(move_dir_right).mulAdd(4, LocationFactory.getTranslation(getFace())),
                    q,
                    new Vector3f(2, 2, 1),
                    new Quaternionf()
            ));
            bd.setBlock(Bukkit.createBlockData(Material.IRON_BLOCK));
        });
        Game.game.markers.add(blockDisplay_left);
    }

    /**
     * @return 方块面对的方向
     */
    private BlockFace getFace() {
        return ((Directional) block.getBlockData()).getFacing();
    }

    public int getFloor() {
        return floor;
    }

    /**
     * 呼叫电梯到对应的楼层
     */
    public void callLift() {
        if (lift.players.isEmpty())
            lift.changeFloorTo(floor);
    }

    public void openDoor() {
        var tr1 = blockDisplay_right.getTransformation();
        blockDisplay_right.setTransformation(new Transformation(
                LocationFactory.getTranslation(getFace()),
                tr1.getLeftRotation(),
                tr1.getScale(),
                tr1.getRightRotation()
        ));
        blockDisplay_right.setInterpolationDelay(0);
        blockDisplay_right.setInterpolationDuration(20);

        var tr2 = blockDisplay_left.getTransformation();
        blockDisplay_left.setTransformation(new Transformation(
                new Vector3f(move_dir_right).mulAdd(4, LocationFactory.getTranslation(getFace())),
                tr2.getLeftRotation(),
                tr2.getScale(),
                tr2.getRightRotation()
        ));
        blockDisplay_left.setInterpolationDelay(0);
        blockDisplay_left.setInterpolationDuration(20);

        LocationFactory.replace2x2Lift(block.getLocation(), Material.AIR, getFace());
    }

    public void closeDoor() {
        var tr1 = blockDisplay_right.getTransformation();
        blockDisplay_right.setTransformation(new Transformation(
                LocationFactory.getTranslation(getFace()).add(move_dir_right),
                tr1.getLeftRotation(),
                tr1.getScale(),
                tr1.getRightRotation()
        ));
        blockDisplay_right.setInterpolationDelay(0);
        blockDisplay_right.setInterpolationDuration(20);

        var tr2 = blockDisplay_left.getTransformation();
        blockDisplay_left.setTransformation(new Transformation(
                new Vector3f(move_dir_right).mulAdd(4, LocationFactory.getTranslation(getFace())).add(move_dir_left),
                tr2.getLeftRotation(),
                tr2.getScale(),
                tr2.getRightRotation()
        ));
        blockDisplay_left.setInterpolationDelay(0);
        blockDisplay_left.setInterpolationDuration(20);

        LocationFactory.replace2x2Lift(block.getLocation(), Material.BARRIER, getFace());
    }
}
