package br.com.devjails;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import br.com.devjails.api.DevJailsAPI;
import br.com.devjails.api.DevJailsAPIImpl;
import br.com.devjails.bail.BailService;
import br.com.devjails.command.DevJailsCommand;
import br.com.devjails.flag.FlagManager;
import br.com.devjails.flag.SelectionService;
import br.com.devjails.handcuff.HandcuffService;
import br.com.devjails.integration.VaultHook;
import br.com.devjails.integration.WorldEditHook;
import br.com.devjails.integration.WorldGuardHook;
import br.com.devjails.jail.JailManager;
import br.com.devjails.listener.MovementListener;
import br.com.devjails.listener.PlayerListener;
import br.com.devjails.listener.RestrictionListener;
import br.com.devjails.listener.SelectionListener;
import br.com.devjails.message.MessageService;
import br.com.devjails.prisoner.PrisonerManager;
import br.com.devjails.storage.SQLiteStorage;
import br.com.devjails.storage.Storage;
import br.com.devjails.storage.YamlStorage;
import br.com.devjails.util.Tasks;
import br.com.devjails.util.LogManager;


public final class DevJailsPlugin extends JavaPlugin {

  private static DevJailsPlugin instance;
  private static DevJailsAPI apiInstance;

  private LogManager logManager;
  private MessageService messageService;
  private Storage storage;
  private VaultHook vaultHook;
  private WorldEditHook worldEditHook;
  private WorldGuardHook worldGuardHook;
  private JailManager jailManager;
  private PrisonerManager prisonerManager;
  private FlagManager flagManager;
  private SelectionService selectionService;
  private HandcuffService handcuffService;
  private BailService bailService;
  private DevJailsAPIImpl api;
  private MovementListener movementListener;

  @Override
  public void onEnable() {
    instance = this;

    logManager = new LogManager(getLogger());
    logManager.startup(getDescription().getVersion());

    Tasks.init(this);
    saveDefaultConfig();
    logManager.configure(getConfig());

    CompletableFuture < Boolean > initChain = CompletableFuture
      .supplyAsync(this::initializeMessageService)
      .thenCompose(success -> {
        if (!success) return CompletableFuture.completedFuture(false);
        return initializeStorage();
      })
      .thenCompose(success -> {
        if (!success) return CompletableFuture.completedFuture(false);
        return initializeIntegrations();
      })
      .thenCompose(success -> {
        if (!success) return CompletableFuture.completedFuture(false);
        return initializeManagers();
      })
      .thenCompose(success -> {
        if (!success) return CompletableFuture.completedFuture(false);
        return initializeAPI();
      })
      .thenApply(success -> {
        if (success) {
          getServer().getScheduler().runTask(this, () -> {
            initializeCommands();
            initializeListeners();
          });

          LogManager.StartupInfo startupInfo = new LogManager.StartupInfo(
            storage.getClass().getSimpleName(),
            vaultHook.isEnabled(),
            worldEditHook.isEnabled(),
            worldGuardHook.isEnabled(),
            prisonerManager.getAllPrisoners().size(),
            jailManager.getAllJails().size()
          );
          logManager.startupComplete(startupInfo);
        }
        return success;
      })
      .exceptionally(throwable -> {
        logManager.error("Critical error during initialization", throwable);
        getServer().getScheduler().runTask(this, () -> {
          getServer().getPluginManager().disablePlugin(this);
        });
        return false;
      });

    initChain.whenComplete((success, throwable) -> {
      if (throwable != null) {
        logManager.error("Error during initialization", throwable);
        getServer().getScheduler().runTask(this, () -> {
          getServer().getPluginManager().disablePlugin(this);
        });
      } else if (!success) {
        logManager.error("DevJails initialization failed - disabling plugin");
        getServer().getScheduler().runTask(this, () -> {
          getServer().getPluginManager().disablePlugin(this);
        });
      }
    });
  }

  @Override
  public void onDisable() {
    try {
      if (prisonerManager != null) {
        prisonerManager.shutdown();
      }
      if (flagManager != null) {
        flagManager.stopAllPreviews();
      }
      if (storage != null) {
        storage.shutdown().join();
      }
    } catch (Exception e) {
      if (logManager != null) {
        logManager.warning("Error during shutdown: " + e.getMessage());
      }
    } finally {
      apiInstance = null;
      instance = null;
      if (logManager != null) {
        logManager.shutdown();
      }
    }
  }

  public static DevJailsPlugin getInstance() {
    return instance;
  }

  public static DevJailsAPI getAPI() {
    return apiInstance;
  }

  private boolean initializeMessageService() {
    try {
      messageService = new MessageService(getDataFolder(), getLogger());

      FileConfiguration config = getConfig();
      String language = config.getString("general.language", "en_US");
      messageService.initialize(language);
      messageService.configureMessages(config);

      logManager.debug("MessageService initialized with language: " + language);
      return true;
    } catch (Exception e) {
      logManager.error("Failed to initialize MessageService", e);
      return false;
    }
  }

  private CompletableFuture < Boolean > initializeStorage() {
    try {
      FileConfiguration config = getConfig();
      String storageType = config.getString("general.storage", "YAML").toUpperCase();

      switch (storageType) {
      case "SQLITE":
        storage = new SQLiteStorage(getDataFolder(), getLogger());
        break;
      case "YAML":
      default:
        storage = new YamlStorage(getDataFolder(), getLogger());
        break;
      }

      logManager.debug("Initializing " + storageType + " storage");
      return storage.initialize();
    } catch (Exception e) {
      logManager.error("Failed to initialize storage", e);
      return CompletableFuture.completedFuture(false);
    }
  }

  private CompletableFuture < Boolean > initializeIntegrations() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        logManager.debug("Initializing integrations...");

        vaultHook = new VaultHook(getLogger());
        worldEditHook = new WorldEditHook(getLogger());
        worldGuardHook = new WorldGuardHook(getLogger());

        boolean vaultResult = vaultHook.initialize();
        boolean worldEditResult = worldEditHook.initialize();
        boolean worldGuardResult = worldGuardHook.initialize();

        logManager.debug("Integration results - Vault: " + vaultResult + 
                        ", WorldEdit: " + worldEditResult + 
                        ", WorldGuard: " + worldGuardResult);

        return true;
      } catch (Exception e) {
        logManager.error("Failed to initialize integrations", e);
        return false;
      }
    });
  }

  private CompletableFuture < Boolean > initializeManagers() {
    try {
      jailManager = new JailManager(storage, worldGuardHook, getLogger());
      prisonerManager = new PrisonerManager(this, storage, jailManager, messageService, getLogger());
      flagManager = new FlagManager(messageService, getLogger());
      selectionService = new SelectionService(worldEditHook, messageService, getLogger());
      handcuffService = new HandcuffService(prisonerManager, messageService, storage, getConfig(), getLogger());
      bailService = new BailService(prisonerManager, vaultHook, messageService, getConfig(), getLogger());

      return CompletableFuture.allOf(
        jailManager.initialize(),
        prisonerManager.initialize()
      ).thenApply(v -> {
        prisonerManager.setHandcuffService(handcuffService);
        logManager.debug("Managers initialized successfully");
        return true;
      });

    } catch (Exception e) {
      logManager.error("Failed to initialize managers", e);
      return CompletableFuture.completedFuture(false);
    }
  }

  private CompletableFuture < Boolean > initializeAPI() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        api = new DevJailsAPIImpl(jailManager, prisonerManager, vaultHook, worldEditHook, worldGuardHook);
        apiInstance = api;

        logManager.debug("Public API registered");
        return true;
      } catch (Exception e) {
        logManager.error("Failed to initialize API", e);
        return false;
      }
    });
  }

  private void initializeCommands() {
    DevJailsCommand devJailsCommand = new DevJailsCommand(this);
    getCommand("djails").setExecutor(devJailsCommand);
    getCommand("djails").setTabCompleter(devJailsCommand);

    logManager.debug("Commands registered");
  }

  private void initializeListeners() {
    movementListener = new MovementListener(this);
    getServer().getPluginManager().registerEvents(movementListener, this);
    getServer().getPluginManager().registerEvents(new RestrictionListener(this), this);
    getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    getServer().getPluginManager().registerEvents(new SelectionListener(this), this);

    logManager.debug("Listeners registrados");

  }

  public MessageService getMessageService() {
    return messageService;
  }

  public Storage getStorage() {
    return storage;
  }

  public VaultHook getVaultHook() {
    return vaultHook;
  }

  public WorldEditHook getWorldEditHook() {
    return worldEditHook;
  }

  public WorldGuardHook getWorldGuardHook() {
    return worldGuardHook;
  }

  public JailManager getJailManager() {
    return jailManager;
  }

  public PrisonerManager getPrisonerManager() {
    return prisonerManager;
  }

  public FlagManager getFlagManager() {
    return flagManager;
  }

  public SelectionService getSelectionService() {
    return selectionService;
  }

  public HandcuffService getHandcuffService() {
    return handcuffService;
  }

  public BailService getBailService() {
    return bailService;
  }

  public MovementListener getMovementListener() {
    return movementListener;
  }

  public CompletableFuture < Boolean > reloadPlugin() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        reloadConfig();

        // Reconfigure MessageService with new language and settings
        FileConfiguration config = getConfig();
        String language = config.getString("general.language", "en_US");
        messageService.initialize(language);
        messageService.configureMessages(config);

        // Reconfigure LogManager with new settings
        logManager.configure(config);

        jailManager.reload().join();
        prisonerManager.reload().join();

        getLogger().info("Plugin recarregado com sucesso!");
        return true;
      } catch (Exception e) {
        getLogger().log(Level.WARNING, "Erro ao recarregar plugin", e);
        return false;
      }
    });
  }
}