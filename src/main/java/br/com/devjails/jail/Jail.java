package br.com.devjails.jail;

import org.bukkit.Bukkit;
import org.bukkit.Location;

/**
 * Representa uma cadeia/prisão no sistema
 */
public class Jail {
    
    /**
     * Tipos de vinculação de área
     */
    public enum AreaBinding {
        NONE("none"),
        FLAG("flag"),
        WORLDGUARD_REGION("worldguard");
        
        private final String value;
        
        AreaBinding(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static AreaBinding fromString(String value) {
            for (AreaBinding binding : values()) {
                if (binding.value.equalsIgnoreCase(value)) {
                    return binding;
                }
            }
            return NONE;
        }
    }
    
    private final String name;
    private Location spawnLocation;
    private AreaBinding areaBinding;
    private String areaRef;
    private JailRestrictions restrictionsOverride;
    
    public Jail(String name, Location spawnLocation) {
        this.name = name;
        this.spawnLocation = spawnLocation;
        this.areaBinding = AreaBinding.NONE;
        this.areaRef = null;
        this.restrictionsOverride = null;
    }
    
    public Jail(String name, Location spawnLocation, AreaBinding areaBinding, String areaRef) {
        this.name = name;
        this.spawnLocation = spawnLocation;
        this.areaBinding = areaBinding;
        this.areaRef = areaRef;
        this.restrictionsOverride = null;
    }
    
    public String getName() {
        return name;
    }
    
    public Location getSpawnLocation() {
        return spawnLocation;
    }
    
    /**
     * Alias para getSpawnLocation (compatibilidade)
     */
    public Location getLocation() {
        return spawnLocation;
    }
    
    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }
    
    public AreaBinding getAreaBinding() {
        return areaBinding;
    }
    
    public void setAreaBinding(AreaBinding areaBinding) {
        this.areaBinding = areaBinding;
    }
    
    public String getAreaRef() {
        return areaRef;
    }
    
    public void setAreaRef(String areaRef) {
        this.areaRef = areaRef;
    }
    
    public JailRestrictions getRestrictionsOverride() {
        return restrictionsOverride;
    }
    
    public void setRestrictionsOverride(JailRestrictions restrictionsOverride) {
        this.restrictionsOverride = restrictionsOverride;
    }
    
    /**
     * Verifica se a jail tem uma área definida
     */
    public boolean hasArea() {
        return areaBinding != AreaBinding.NONE && areaRef != null && !areaRef.isEmpty();
    }
    
    /**
     * Restrições específicas da cadeia (sobrescreve as globais)
     */
    public static class JailRestrictions {
        private Boolean blockCommands;
        private Boolean blockBreak;
        private Boolean blockPlace;
        private Boolean blockInteract;
        private Boolean blockChat;
        private Boolean pvpEnabled;
        
        public Boolean getBlockCommands() {
            return blockCommands;
        }
        
        public void setBlockCommands(Boolean blockCommands) {
            this.blockCommands = blockCommands;
        }
        
        public Boolean getBlockBreak() {
            return blockBreak;
        }
        
        public void setBlockBreak(Boolean blockBreak) {
            this.blockBreak = blockBreak;
        }
        
        public Boolean getBlockPlace() {
            return blockPlace;
        }
        
        public void setBlockPlace(Boolean blockPlace) {
            this.blockPlace = blockPlace;
        }
        
        public Boolean getBlockInteract() {
            return blockInteract;
        }
        
        public void setBlockInteract(Boolean blockInteract) {
            this.blockInteract = blockInteract;
        }
        
        public Boolean getBlockChat() {
            return blockChat;
        }
        
        public void setBlockChat(Boolean blockChat) {
            this.blockChat = blockChat;
        }
        
        public Boolean getPvpEnabled() {
            return pvpEnabled;
        }
        
        public void setPvpEnabled(Boolean pvpEnabled) {
            this.pvpEnabled = pvpEnabled;
        }
    }
    
    @Override
    public String toString() {
        return "Jail{" +
                "name='" + name + '\'' +
                ", spawnLocation=" + spawnLocation +
                ", areaBinding=" + areaBinding +
                ", areaRef='" + areaRef + '\'' +
                '}';
    }
}