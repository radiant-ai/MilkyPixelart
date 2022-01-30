package fun.milkyway.milkypixelart.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class Utils {
    public static Location calculatePlayerFace(Player p) {
        double x = p.getLocation().getX();
        double y = p.getLocation().getY() + 1.0;
        double z = p.getLocation().getZ();
        Vector faceDirection = translateDirectionToVector(getDirection(p.getLocation()));
        Location faceLocation = new Location(p.getWorld(), x, y, z);
        faceLocation.add(faceDirection.multiply(1.5));
        faceLocation.setYaw((float) translateDirectionToYaw(flipDirection(getDirection(p.getLocation()))));
        return faceLocation;
    }

    public static int flipDirection(int direction) {
        switch (direction) {
            case 2:
                return 3;
            case 3:
                return 2;
            case 4:
                return 5;
            case 5:
                return 4;
            default:
                return 0;
        }
    }

    public static int getDirection(Location l) {
        float rot = l.getYaw();
        if (rot < 0) {
            rot += 360.0f;
        }
        if (0 <= rot && rot < 45.0) {
            return 3;
        } else if (45.0 <= rot && rot < 135.0) {
            return 4;
        } else if (135.0 <= rot && rot < 225.0) {
            return 2;
        } else if (225.0 <= rot && rot < 315.0) {
            return 5;
        } else if (315.0 <= rot && rot < 360.0) {
            return 3;
        } else {
            return 2;
        }
    }

    public static float alignYaw(Location l) {
        float rot = l.getYaw();
        if (rot < 0) {
            rot += 360.0f;
        }
        if (0 <= rot && rot < 45.0) {
            return 0.0f;
        } else if (45.0 <= rot && rot < 135.0) {
            return 90.0f;
        } else if (135.0 <= rot && rot < 225.0) {
            return 180.0f;
        } else if (225.0 <= rot && rot < 315.0) {
            return 270.0f;
        } else if (315.0 <= rot && rot < 360.0) {
            return 0.0f;
        } else {
            return 0.0f;
        }
    }

    public static Vector translateDirectionToVector(int direction) {
        switch (direction) {
            case 2:
                return new Vector(0,0,-1);
            case 3:
                return new Vector(0,0,1);
            case 4:
                return new Vector(-1,0,0);
            case 5:
                return new Vector(1,0,0);
            default:
                return new Vector(0,0,0);
        }
    }

    public static double translateDirectionToYaw(int direction) {
        switch (direction) {
            case 2:
                return 180.0;
            case 3:
                return 0.1;
            case 4:
                return 90.0;
            case 5:
                return 270.0;
            default:
                return 0.0;
        }
    }
}
