package fun.milkyway.milkypixelart.utils;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class OrientationUtils {
    public static List<Location>  calculateGridInFrontOfPlayer(Location location, double distance, int width, int height) {
        var playerBlock = location.toBlockLocation();
        var direction = nearestVector(location.getYaw());
        var gridMiddle = playerBlock.add(0, 1.7, 0).add(direction.multiply(distance)).toBlockLocation();
        if (width == 1 && height == 1) {
            return List.of(gridMiddle);
        }
        var grid = new ArrayList<Location>(width * height);
        var upVector = new Vector(0, 1, 0);
        var leftVector = upVector.clone().crossProduct(direction).normalize();
        var firstPoint = gridMiddle.add(upVector.clone().multiply(height / 2)).add(leftVector.clone().multiply(width / 2));
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                var point = firstPoint.clone().add(upVector.clone().multiply(-i)).add(leftVector.clone().multiply(-j));
                grid.add(point);
            }
        }
        return grid;
    }

    public static BlockFace calculateOpositeBlockFace(double yaw) {
        var yawInt = nearestYaw(yaw);
        return switch (yawInt) {
            case 90 -> BlockFace.EAST;
            case 180 -> BlockFace.SOUTH;
            case 270 -> BlockFace.WEST;
            default -> BlockFace.NORTH;
        };
    }

    public static Vector nearestVector(double yaw) {
        var yawInt = nearestYaw(yaw);
        return switch (yawInt) {
            case 90 -> new Vector(-1, 0, 0);
            case 180 -> new Vector(0, 0, -1);
            case 270 -> new Vector(1, 0, 0);
            default -> new Vector(0, 0, 1);
        };
    }

    public static int nearestYaw(double yaw) {
        double normalizedYaw = ((yaw % 360) + 360) % 360;

        if (normalizedYaw < 45 || normalizedYaw > 315) {
            return 0;
        }
        else if (normalizedYaw < 135) {
            return 90;
        }
        else if (normalizedYaw < 225) {
            return 180;
        }
        else {
            return 270;
        }
    }
}
