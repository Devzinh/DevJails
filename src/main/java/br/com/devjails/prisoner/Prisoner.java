package br.com.devjails.prisoner;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/**
 * Representa um prisioneiro no sistema
 */
public class Prisoner {
    
    private final UUID uuid;
    private String jailName;
    private String reason;
    private String staff;
    private long startEpoch;
    private Long endEpoch; 
    private Double bailAmount;
    private boolean bailEnabled;
    private Location originalLocation;
    private PostReleaseSpawnChoice postReleaseSpawnChoice;
    private boolean handcuffed;
    
    // Campos para rastreamento de tempo online
    private long onlineTimeAccumulated; // Tempo online acumulado em milissegundos
    private Long lastOnlineTime; // Timestamp da última vez que ficou online
    private boolean isCurrentlyOnline; // Se o jogador está atualmente online
    
    public Prisoner(UUID uuid, String jailName, String reason, String staff) {
        this.uuid = uuid;
        this.jailName = jailName;
        this.reason = reason;
        this.staff = staff;
        this.startEpoch = System.currentTimeMillis();
        this.endEpoch = null; 
        this.bailAmount = null;
        this.bailEnabled = false;
        this.originalLocation = null;
        this.postReleaseSpawnChoice = PostReleaseSpawnChoice.WORLD_SPAWN;
        this.handcuffed = false;
        
        // Inicializar campos de tempo online
        this.onlineTimeAccumulated = 0;
        this.lastOnlineTime = null;
        this.isCurrentlyOnline = Bukkit.getPlayer(uuid) != null;
        if (this.isCurrentlyOnline) {
            this.lastOnlineTime = System.currentTimeMillis();
        }
    }
    
    public Prisoner(UUID uuid, String jailName, String reason, String staff, long durationMillis) {
        this(uuid, jailName, reason, staff);
        this.endEpoch = this.startEpoch + durationMillis;
    }
    
    public UUID getUuid() {
        return uuid;
    }
    
    public String getJailName() {
        return jailName;
    }
    
    public void setJailName(String jailName) {
        this.jailName = jailName;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getStaff() {
        return staff;
    }
    
    public void setStaff(String staff) {
        this.staff = staff;
    }
    
    public long getStartEpoch() {
        return startEpoch;
    }
    
    public void setStartEpoch(long startEpoch) {
        this.startEpoch = startEpoch;
    }
    
    public Long getEndEpoch() {
        return endEpoch;
    }
    
    public void setEndEpoch(Long endEpoch) {
        this.endEpoch = endEpoch;
    }
    
    public Double getBailAmount() {
        return bailAmount;
    }
    
    public void setBailAmount(Double bailAmount) {
        this.bailAmount = bailAmount;
    }
    
    public boolean isBailEnabled() {
        return bailEnabled;
    }
    
    public void setBailEnabled(boolean bailEnabled) {
        this.bailEnabled = bailEnabled;
    }
    
    public Location getOriginalLocation() {
        return originalLocation;
    }
    
    public void setOriginalLocation(Location originalLocation) {
        this.originalLocation = originalLocation;
    }
    
    public PostReleaseSpawnChoice getPostReleaseSpawnChoice() {
        return postReleaseSpawnChoice;
    }
    
    public void setPostReleaseSpawnChoice(PostReleaseSpawnChoice postReleaseSpawnChoice) {
        this.postReleaseSpawnChoice = postReleaseSpawnChoice;
    }
    
    public boolean isHandcuffed() {
        return handcuffed;
    }
    
    public void setHandcuffed(boolean handcuffed) {
        this.handcuffed = handcuffed;
    }
    
    // Métodos para rastreamento de tempo online
    public long getOnlineTimeAccumulated() {
        return onlineTimeAccumulated;
    }
    
    public void setOnlineTimeAccumulated(long onlineTimeAccumulated) {
        this.onlineTimeAccumulated = onlineTimeAccumulated;
    }
    
    public Long getLastOnlineTime() {
        return lastOnlineTime;
    }
    
    public void setLastOnlineTime(Long lastOnlineTime) {
        this.lastOnlineTime = lastOnlineTime;
    }
    
    public boolean isCurrentlyOnline() {
        return isCurrentlyOnline;
    }
    
    public void setCurrentlyOnline(boolean currentlyOnline) {
        this.isCurrentlyOnline = currentlyOnline;
    }
    
    /**
     * Verifica se a prisÃ£o Ã© permanente
     */
    public boolean isPermanent() {
        return endEpoch == null;
    }
    
    /**
     * Verifica se a prisÃ£o jÃ¡ expirou (baseado no tempo online)
     */
    public boolean isExpired() {
        if (isPermanent()) {
            return false;
        }
        long totalOnlineTime = getTotalOnlineTime();
        long durationMillis = endEpoch - startEpoch;
        return totalOnlineTime >= durationMillis;
    }
    
    /**
     * ObtÃ©m o tempo restante em milissegundos (baseado no tempo online)
     * @return tempo restante ou -1 se for permanente
     */
    public long getRemainingTimeMillis() {
        if (isPermanent()) {
            return -1;
        }
        long totalOnlineTime = getTotalOnlineTime();
        long durationMillis = endEpoch - startEpoch;
        long remaining = durationMillis - totalOnlineTime;
        return Math.max(0, remaining);
    }
    
    /**
     * Verifica se tem Fiança disponÃ­vel
     */
    public boolean hasBail() {
        return bailEnabled && bailAmount != null && bailAmount > 0;
    }
    
    /**
     * Estende o tempo de prisÃ£o
     */
    public void extendTime(long additionalMillis) {
        if (isPermanent()) {

            this.endEpoch = System.currentTimeMillis() + additionalMillis;
        } else {

            this.endEpoch += additionalMillis;
        }
    }
    
    /**
     * Marca o jogador como online e inicia a contagem de tempo
     */
    public void markOnline() {
        if (!isCurrentlyOnline) {
            isCurrentlyOnline = true;
            lastOnlineTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Marca o jogador como offline e acumula o tempo online
     */
    public void markOffline() {
        if (isCurrentlyOnline && lastOnlineTime != null) {
            long sessionTime = System.currentTimeMillis() - lastOnlineTime;
            onlineTimeAccumulated += sessionTime;
            isCurrentlyOnline = false;
            lastOnlineTime = null;
        }
    }
    
    /**
     * Atualiza o tempo online acumulado se o jogador estiver online
     */
    public void updateOnlineTime() {
        if (isCurrentlyOnline && lastOnlineTime != null) {
            long sessionTime = System.currentTimeMillis() - lastOnlineTime;
            onlineTimeAccumulated += sessionTime;
            lastOnlineTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Obtém o tempo total online (acumulado + sessão atual se estiver online)
     * @return tempo total online em milissegundos
     */
    public long getTotalOnlineTime() {
        long total = onlineTimeAccumulated;
        if (isCurrentlyOnline && lastOnlineTime != null) {
            total += System.currentTimeMillis() - lastOnlineTime;
        }
        return total;
    }
    
    /**
     * OpÃ§Ãµes de spawn apÃ³s soltura
     */
    public enum PostReleaseSpawnChoice {
        WORLD_SPAWN("world_spawn"),
        ORIGINAL_LOCATION("original_location");
        
        private final String value;
        
        PostReleaseSpawnChoice(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static PostReleaseSpawnChoice fromString(String value) {
            for (PostReleaseSpawnChoice choice : values()) {
                if (choice.value.equalsIgnoreCase(value)) {
                    return choice;
                }
            }
            return WORLD_SPAWN;
        }
    }
    public UUID getPlayerId() {
        return uuid;
    }
    public String getPlayerName() {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() != null ? player.getName() : "Desconhecido";
    }
    
    @Override
    public String toString() {
        return "Prisoner{" +
                "uuid=" + uuid +
                ", jailName='" + jailName + '\'' +
                ", reason='" + reason + '\'' +
                ", staff='" + staff + '\'' +
                ", startEpoch=" + startEpoch +
                ", endEpoch=" + endEpoch +
                ", bailAmount=" + bailAmount +
                ", bailEnabled=" + bailEnabled +
                ", handcuffed=" + handcuffed +
                '}';
    }
}