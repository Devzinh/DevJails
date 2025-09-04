package br.com.devjails.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Sistema de logging profissional para o DevJails
 * Controla níveis de verbosidade e formatação de mensagens
 */
public class LogManager {
    
    private final Logger logger;
    private boolean verboseMode;
    private boolean debugMode;
    private boolean startupSummary;
    private boolean minimalStartup;
    
    public LogManager(Logger logger) {
        this.logger = logger;
        this.verboseMode = false;
        this.debugMode = false;
        this.startupSummary = true;
        this.minimalStartup = false;
    }
    
    public void configure(FileConfiguration config) {
        // Configurações de logging do sistema
        this.verboseMode = config.getBoolean("logging.verbose", false);
        this.debugMode = config.getBoolean("logging.debug", false);
        this.startupSummary = config.getBoolean("logging.startup-summary", true);
        this.minimalStartup = config.getBoolean("logging.minimal-startup", false);
        
        // Verificar também a configuração geral de debug para compatibilidade
        boolean generalDebug = config.getBoolean("general.debug", false);
        if (generalDebug && !this.debugMode) {
            this.debugMode = true;
        }
    }
    
    /**
     * Log de inicialização do plugin
     */
    public void startup(String version) {
        if (!minimalStartup) {
            logger.info("DevJails v" + version + " starting...");
        }
    }
    
    /**
     * Log de conclusão do startup
     */
    public void startupComplete(StartupInfo info) {
        if (startupSummary && !minimalStartup) {
            logger.info("DevJails loaded successfully! | Storage: " + info.storageType + " | " +
                       "Vault: " + (info.vaultEnabled ? "✓" : "✗") + " | " +
                       "WorldEdit: " + (info.worldEditEnabled ? "✓" : "✗") + " | " +
                       "WorldGuard: " + (info.worldGuardEnabled ? "✓" : "✗") +
                       (info.prisonerCount > 0 ? " | Loaded " + info.prisonerCount + " prisoners and " + info.jailCount + " jails" : ""));
        } else if (!minimalStartup) {
            logger.info("DevJails loaded successfully!");
        }
    }
    
    /**
     * Log de operações administrativas (sempre visível)
     */
    public void admin(String message) {
        logger.info("[ADMIN] " + message);
    }
    
    /**
     * Log de operações do sistema (apenas em modo verbose)
     */
    public void system(String message) {
        if (verboseMode) {
            logger.info("[SYSTEM] " + message);
        }
    }
    
    /**
     * Log de debug (apenas em modo debug)
     */
    public void debug(String message) {
        if (debugMode) {
            logger.info("[DEBUG] " + message);
        }
    }
    
    /**
     * Log de avisos
     */
    public void warning(String message) {
        logger.warning(message);
    }
    
    /**
     * Log de erros
     */
    public void error(String message) {
        logger.severe(message);
    }
    
    /**
     * Log de erros com exceção
     */
    public void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }
    
    /**
     * Log de shutdown
     */
    public void shutdown() {
        logger.info("DevJails disabled");
    }
    
    /**
     * Informações de startup para resumo
     */
    public static class StartupInfo {
        public String storageType;
        public boolean vaultEnabled;
        public boolean worldEditEnabled;
        public boolean worldGuardEnabled;
        public int prisonerCount;
        public int jailCount;
        
        public StartupInfo(String storageType, boolean vaultEnabled, boolean worldEditEnabled, 
                          boolean worldGuardEnabled, int prisonerCount, int jailCount) {
            this.storageType = storageType;
            this.vaultEnabled = vaultEnabled;
            this.worldEditEnabled = worldEditEnabled;
            this.worldGuardEnabled = worldGuardEnabled;
            this.prisonerCount = prisonerCount;
            this.jailCount = jailCount;
        }
    }
}