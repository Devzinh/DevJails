package br.com.devjails.storage;

import br.com.devjails.flag.FlagRegion;
import br.com.devjails.jail.Jail;
import br.com.devjails.prisoner.Prisoner;
import br.com.devjails.util.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Implementação de storage usando SQLite
 */
public class SQLiteStorage extends AbstractStorage {
    
    private Connection connection;
    
    public SQLiteStorage(File dataFolder, Logger logger) {
        super(dataFolder, logger);
    }
    
    @Override
    protected String getStorageType() {
        return "SQLiteStorage";
    }
    
    @Override
    protected boolean doInitialize() throws Exception {
        File dbFile = new File(dataFolder, "DevJails.db");
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        
        connection = DriverManager.getConnection(url);
        connection.setAutoCommit(true);
        
        createTables();
        return true;
    }
    
    @Override
    protected void doShutdown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
    
    @Override
    protected boolean doHealthCheck() throws Exception {
        return connection != null && !connection.isClosed();
    }
    
    @Override
    protected List<String> getAllJailNames() throws Exception {
        List<String> names = new ArrayList<>();
        String sql = "SELECT name FROM jails";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        }
        
        return names;
    }
    
    @Override
    protected List<UUID> getAllPrisonerUuids() throws Exception {
        List<UUID> uuids = new ArrayList<>();
        String sql = "SELECT uuid FROM prisoners";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                try {
                    uuids.add(UUID.fromString(rs.getString("uuid")));
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid UUID found: " + rs.getString("uuid"));
                }
            }
        }
        
        return uuids;
    }
    
    @Override
    protected List<String> getAllFlagNames() throws Exception {
        List<String> names = new ArrayList<>();
        String sql = "SELECT name FROM flags";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        }
        
        return names;
    }
    
    private void createTables() throws SQLException {

        String createJailsTable = """
            CREATE TABLE IF NOT EXISTS jails (
                name TEXT PRIMARY KEY,
                world TEXT NOT NULL,
                x REAL NOT NULL,
                y REAL NOT NULL,
                z REAL NOT NULL,
                yaw REAL NOT NULL,
                pitch REAL NOT NULL,
                area_binding TEXT DEFAULT 'none',
                area_ref TEXT,
                restrictions_json TEXT
            )
        """;

        String createPrisonersTable = """
            CREATE TABLE IF NOT EXISTS prisoners (
                uuid TEXT PRIMARY KEY,
                jail_name TEXT NOT NULL,
                reason TEXT NOT NULL,
                staff TEXT NOT NULL,
                start_epoch INTEGER NOT NULL,
                end_epoch INTEGER,
                bail_amount REAL,
                bail_enabled INTEGER DEFAULT 0,
                handcuffed INTEGER DEFAULT 0,
                post_release_spawn_choice TEXT DEFAULT 'world_spawn',
                original_world TEXT,
                original_x REAL,
                original_y REAL,
                original_z REAL,
                original_yaw REAL,
                original_pitch REAL
            )
        """;

        String createFlagsTable = """
            CREATE TABLE IF NOT EXISTS flags (
                name TEXT PRIMARY KEY,
                world_name TEXT NOT NULL,
                min_x INTEGER NOT NULL,
                min_y INTEGER NOT NULL,
                min_z INTEGER NOT NULL,
                max_x INTEGER NOT NULL,
                max_y INTEGER NOT NULL,
                max_z INTEGER NOT NULL
            )
        """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createJailsTable);
            stmt.execute(createPrisonersTable);
            stmt.execute(createFlagsTable);
        }
    }    
    
    @Override
    public CompletableFuture<Boolean> saveJail(Jail jail) {
        return Tasks.asyncThenSync(() -> {
            String sql = """
                INSERT OR REPLACE INTO jails 
                (name, world, x, y, z, yaw, pitch, area_binding, area_ref, restrictions_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                Location spawn = jail.getSpawnLocation();
                
                stmt.setString(1, jail.getName());
                stmt.setString(2, spawn.getWorld().getName());
                stmt.setDouble(3, spawn.getX());
                stmt.setDouble(4, spawn.getY());
                stmt.setDouble(5, spawn.getZ());
                stmt.setFloat(6, spawn.getYaw());
                stmt.setFloat(7, spawn.getPitch());
                stmt.setString(8, jail.getAreaBinding().getValue());
                stmt.setString(9, jail.getAreaRef());
                stmt.setString(10, null); 
                
                stmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                logger.severe("Error saving jail " + jail.getName() + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, null);
    }
    
    @Override
    public CompletableFuture<Jail> loadJail(String name) {
        return Tasks.asyncThenSync(() -> {
            String sql = "SELECT * FROM jails WHERE name = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, name);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String world = rs.getString("world");
                        double x = rs.getDouble("x");
                        double y = rs.getDouble("y");
                        double z = rs.getDouble("z");
                        float yaw = rs.getFloat("yaw");
                        float pitch = rs.getFloat("pitch");
                        
                        Location spawn = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
                        
                        Jail.AreaBinding areaBinding = Jail.AreaBinding.fromString(
                            rs.getString("area_binding")
                        );
                        String areaRef = rs.getString("area_ref");
                        
                        return new Jail(name, spawn, areaBinding, areaRef);
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error loading jail " + name + ": " + e.getMessage());
                e.printStackTrace();
            }
            
            return null;
        }, null);
    }
    
    @Override
    public CompletableFuture<Boolean> removeJail(String name) {
        return Tasks.asyncThenSync(() -> {
            String sql = "DELETE FROM jails WHERE name = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                logger.severe("Error removing jail " + name + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, null);
    }
    
    
    @Override
    public CompletableFuture<Boolean> savePrisoner(Prisoner prisoner) {
        return Tasks.asyncThenSync(() -> {
            String sql = """
                INSERT OR REPLACE INTO prisoners 
                (uuid, jail_name, reason, staff, start_epoch, end_epoch, bail_amount, bail_enabled, 
                 handcuffed, post_release_spawn_choice, original_world, original_x, original_y, 
                 original_z, original_yaw, original_pitch)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, prisoner.getUuid().toString());
                stmt.setString(2, prisoner.getJailName());
                stmt.setString(3, prisoner.getReason());
                stmt.setString(4, prisoner.getStaff());
                stmt.setLong(5, prisoner.getStartEpoch());
                
                if (prisoner.getEndEpoch() != null) {
                    stmt.setLong(6, prisoner.getEndEpoch());
                } else {
                    stmt.setNull(6, Types.INTEGER);
                }
                
                if (prisoner.getBailAmount() != null) {
                    stmt.setDouble(7, prisoner.getBailAmount());
                } else {
                    stmt.setNull(7, Types.REAL);
                }
                
                stmt.setInt(8, prisoner.isBailEnabled() ? 1 : 0);
                stmt.setInt(9, prisoner.isHandcuffed() ? 1 : 0);
                stmt.setString(10, prisoner.getPostReleaseSpawnChoice().getValue());
                
                Location originalLoc = prisoner.getOriginalLocation();
                if (originalLoc != null) {
                    stmt.setString(11, originalLoc.getWorld().getName());
                    stmt.setDouble(12, originalLoc.getX());
                    stmt.setDouble(13, originalLoc.getY());
                    stmt.setDouble(14, originalLoc.getZ());
                    stmt.setFloat(15, originalLoc.getYaw());
                    stmt.setFloat(16, originalLoc.getPitch());
                } else {
                    stmt.setNull(11, Types.VARCHAR);
                    stmt.setNull(12, Types.REAL);
                    stmt.setNull(13, Types.REAL);
                    stmt.setNull(14, Types.REAL);
                    stmt.setNull(15, Types.REAL);
                    stmt.setNull(16, Types.REAL);
                }
                
                stmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                logger.severe("Error saving prisoner " + prisoner.getUuid() + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, null);
    }
    
    @Override
    public CompletableFuture<Prisoner> loadPrisoner(UUID uuid) {
        return Tasks.asyncThenSync(() -> {
            String sql = "SELECT * FROM prisoners WHERE uuid = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String jailName = rs.getString("jail_name");
                        String reason = rs.getString("reason");
                        String staff = rs.getString("staff");
                        long startEpoch = rs.getLong("start_epoch");
                        Long endEpoch = rs.getObject("end_epoch", Long.class);
                        
                        Prisoner prisoner = new Prisoner(uuid, jailName, reason, staff);
                        prisoner.setStartEpoch(startEpoch);
                        prisoner.setEndEpoch(endEpoch);
                        
                        Double bailAmount = rs.getObject("bail_amount", Double.class);
                        if (bailAmount != null) {
                            prisoner.setBailAmount(bailAmount);
                        }
                        
                        prisoner.setBailEnabled(rs.getInt("bail_enabled") == 1);
                        prisoner.setHandcuffed(rs.getInt("handcuffed") == 1);
                        
                        String spawnChoiceStr = rs.getString("post_release_spawn_choice");
                        prisoner.setPostReleaseSpawnChoice(
                            Prisoner.PostReleaseSpawnChoice.fromString(spawnChoiceStr)
                        );
                        String originalWorld = rs.getString("original_world");
                        if (originalWorld != null) {
                            double x = rs.getDouble("original_x");
                            double y = rs.getDouble("original_y");
                            double z = rs.getDouble("original_z");
                            float yaw = rs.getFloat("original_yaw");
                            float pitch = rs.getFloat("original_pitch");
                            
                            Location originalLoc = new Location(Bukkit.getWorld(originalWorld), x, y, z, yaw, pitch);
                            prisoner.setOriginalLocation(originalLoc);
                        }
                        
                        return prisoner;
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error loading prisoner " + uuid + ": " + e.getMessage());
                e.printStackTrace();
            }
            
            return null;
        }, null);
    }
    
    @Override
    public CompletableFuture<Boolean> removePrisoner(UUID uuid) {
        return Tasks.asyncThenSync(() -> {
            String sql = "DELETE FROM prisoners WHERE uuid = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                logger.severe("Error removing prisoner " + uuid + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, null);
    }
    

    
    @Override
    public CompletableFuture<List<Prisoner>> loadPrisonersByJail(String jailName) {
        return Tasks.asyncThenSync(() -> {
            List<Prisoner> prisoners = new ArrayList<>();
            String sql = "SELECT uuid FROM prisoners WHERE jail_name = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, jailName);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        Prisoner prisoner = loadPrisoner(uuid).join();
                        if (prisoner != null) {
                            prisoners.add(prisoner);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error loading prisoners from jail " + jailName + ": " + e.getMessage());
                e.printStackTrace();
            }
            
            return prisoners;
        }, null);
    }    
    
    @Override
    public CompletableFuture<Boolean> saveFlag(FlagRegion flag) {
        return Tasks.asyncThenSync(() -> {
            String sql = """
                INSERT OR REPLACE INTO flags 
                (name, world_name, min_x, min_y, min_z, max_x, max_y, max_z)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                FlagRegion.Vector3 min = flag.getRegion().getMin();
                FlagRegion.Vector3 max = flag.getRegion().getMax();
                
                stmt.setString(1, flag.getName());
                stmt.setString(2, flag.getWorldName());
                stmt.setInt(3, min.getX());
                stmt.setInt(4, min.getY());
                stmt.setInt(5, min.getZ());
                stmt.setInt(6, max.getX());
                stmt.setInt(7, max.getY());
                stmt.setInt(8, max.getZ());
                
                stmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                logger.severe("Error saving flag " + flag.getName() + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, null);
    }
    
    @Override
    public CompletableFuture<FlagRegion> loadFlag(String name) {
        return Tasks.asyncThenSync(() -> {
            String sql = "SELECT * FROM flags WHERE name = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, name);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String worldName = rs.getString("world_name");
                        
                        int minX = rs.getInt("min_x");
                        int minY = rs.getInt("min_y");
                        int minZ = rs.getInt("min_z");
                        int maxX = rs.getInt("max_x");
                        int maxY = rs.getInt("max_y");
                        int maxZ = rs.getInt("max_z");
                        
                        FlagRegion.Vector3 min = new FlagRegion.Vector3(minX, minY, minZ);
                        FlagRegion.Vector3 max = new FlagRegion.Vector3(maxX, maxY, maxZ);
                        FlagRegion.CuboidRegion region = new FlagRegion.CuboidRegion(min, max);
                        
                        return new FlagRegion(name, worldName, region);
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error loading flag " + name + ": " + e.getMessage());
                e.printStackTrace();
            }
            
            return null;
        }, null);
    }
    
    @Override
    public CompletableFuture<Boolean> removeFlag(String name) {
        return Tasks.asyncThenSync(() -> {
            String sql = "DELETE FROM flags WHERE name = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                logger.severe("Error removing flag " + name + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, null);
    }
    
    
}