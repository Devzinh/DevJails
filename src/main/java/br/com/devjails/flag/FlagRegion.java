package br.com.devjails.flag;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class FlagRegion {
    
    private final String name;
    private String worldName;
    private CuboidRegion region;
    
    public FlagRegion(String name, String worldName, CuboidRegion region) {
        this.name = name;
        this.worldName = worldName;
        this.region = region;
    }
    
    public FlagRegion(String name, Location pos1, Location pos2) {
        this.name = name;
        
        if (pos1.getWorld() == null || pos2.getWorld() == null) {
            throw new IllegalArgumentException("Locations must have a valid world");
        }
        
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            throw new IllegalArgumentException("Both locations must be in the same world");
        }
        
        this.worldName = pos1.getWorld().getName();
        this.region = new CuboidRegion(pos1, pos2);
    }
    
    public String getName() {
        return name;
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }
    
    public CuboidRegion getRegion() {
        return region;
    }
    
    public void setRegion(CuboidRegion region) {
        this.region = region;
    }
    
    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public boolean contains(Location location) {
        if (location.getWorld() == null || !location.getWorld().getName().equals(worldName)) {
            return false;
        }
        
        return region.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
    public Location getCenter() {
        World world = getWorld();
        if (world == null) {
            return null;
        }
        
        Vector3 center = region.getCenter();
        return new Location(world, center.getX(), center.getY(), center.getZ());
    }
    public int getVolume() {
        return region.getVolume();
    }

    public static class CuboidRegion {
        private Vector3 min;
        private Vector3 max;
        
        public CuboidRegion(Vector3 min, Vector3 max) {

            this.min = Vector3.min(min, max);
            this.max = Vector3.max(min, max);
        }
        
        public CuboidRegion(Location pos1, Location pos2) {
            this(new Vector3(pos1), new Vector3(pos2));
        }
        
        public Vector3 getMin() {
            return min;
        }
        
        public void setMin(Vector3 min) {
            this.min = min;
        }
        
        public Vector3 getMax() {
            return max;
        }
        
        public void setMax(Vector3 max) {
            this.max = max;
        }
        
        public boolean contains(int x, int y, int z) {
            return x >= min.getX() && x <= max.getX() &&
                   y >= min.getY() && y <= max.getY() &&
                   z >= min.getZ() && z <= max.getZ();
        }
        
        public Vector3 getCenter() {
            return new Vector3(
                (min.getX() + max.getX()) / 2.0,
                (min.getY() + max.getY()) / 2.0,
                (min.getZ() + max.getZ()) / 2.0
            );
        }
        
        public int getVolume() {
            return (max.getX() - min.getX() + 1) *
                   (max.getY() - min.getY() + 1) *
                   (max.getZ() - min.getZ() + 1);
        }
    }

    public static class Vector3 {
        private final int x;
        private final int y;
        private final int z;
        
        public Vector3(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public Vector3(double x, double y, double z) {
            this.x = (int) Math.floor(x);
            this.y = (int) Math.floor(y);
            this.z = (int) Math.floor(z);
        }
        
        public Vector3(Location location) {
            this(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }
        
        public int getX() {
            return x;
        }
        
        public int getY() {
            return y;
        }
        
        public int getZ() {
            return z;
        }
        
        public static Vector3 min(Vector3 v1, Vector3 v2) {
            return new Vector3(
                Math.min(v1.x, v2.x),
                Math.min(v1.y, v2.y),
                Math.min(v1.z, v2.z)
            );
        }
        
        public static Vector3 max(Vector3 v1, Vector3 v2) {
            return new Vector3(
                Math.max(v1.x, v2.x),
                Math.max(v1.y, v2.y),
                Math.max(v1.z, v2.z)
            );
        }
        
        @Override
        public String toString() {
            return "(" + x + ", " + y + ", " + z + ")";
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Vector3 vector3 = (Vector3) obj;
            return x == vector3.x && y == vector3.y && z == vector3.z;
        }
        
        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            result = 31 * result + z;
            return result;
        }
    }
    
    @Override
    public String toString() {
        return "FlagRegion{" +
                "name='" + name + '\'' +
                ", worldName='" + worldName + '\'' +
                ", min=" + region.getMin() +
                ", max=" + region.getMax() +
                '}';
    }
}