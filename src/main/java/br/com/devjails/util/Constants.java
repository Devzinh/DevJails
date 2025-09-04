package br.com.devjails.util;

/**
 * Classe de constantes para centralizar valores comuns usados no projeto
 */
public final class Constants {
    
    // Previne instanciação
    private Constants() {
        throw new UnsupportedOperationException("Esta é uma classe utilitária e não pode ser instanciada");
    }
    
    /**
     * Constantes relacionadas a GUI
     */
    public static final class GUI {
        public static final int PRISONERS_ITEMS_PER_PAGE = 45;
        public static final int PRISONERS_GUI_SIZE = 54;
        public static final int BAIL_ITEMS_PER_PAGE = 28;
        public static final int BAIL_GUI_SIZE = 54;
        public static final int PRISONER_OPTIONS_GUI_SIZE = 27;
        
        // Slots especiais para navegação
        public static final int PREVIOUS_PAGE_SLOT = 45;
        public static final int NEXT_PAGE_SLOT = 53;
        public static final int CLOSE_SLOT = 49;
        public static final int INFO_SLOT = 4;
    }
    
    /**
     * Constantes de tempo
     */
    public static final class Time {
        public static final long SECOND_MILLIS = 1000L;
        public static final long MINUTE_MILLIS = 60L * SECOND_MILLIS;
        public static final long HOUR_MILLIS = 60L * MINUTE_MILLIS;
        public static final long DAY_MILLIS = 24L * HOUR_MILLIS;
        public static final long WEEK_MILLIS = 7L * DAY_MILLIS;
        
        // Cooldowns
        public static final long ESCAPE_COOLDOWN_MS = 1000L; // 1 segundo para detecção de fuga
        public static final long INTERACTION_COOLDOWN_MS = 3000L;
        
        // Ticks (20 ticks = 1 segundo)
        public static final long TICKS_PER_SECOND = 20L;
        public static final long TICKS_PER_MINUTE = 60L * TICKS_PER_SECOND;
    }
    
    /**
     * Constantes de configuração
     */
    public static final class Config {
        // Seções principais
        public static final String GENERAL_SECTION = "general";
        public static final String JAIL_SECTION = "jail";
        public static final String BAIL_SECTION = "bail";
        public static final String RESTRICTIONS_SECTION = "restrictions";
        public static final String HANDCUFFS_SECTION = "handcuffs";
        public static final String AREAS_SECTION = "areas";
        public static final String RELEASE_SPAWN_SECTION = "release-spawn";
        
        // Chaves de configuração geral
        public static final String LANGUAGE_KEY = "general.language";
        public static final String STORAGE_KEY = "general.storage";
        
        // Chaves de configuração de prisão
        public static final String DEFAULT_TEMP_DURATION_KEY = "jail.default-temp-duration";
        public static final String TELEPORT_BACK_ON_ESCAPE_KEY = "jail.escape.teleport-back";
        public static final String FINE_ON_ESCAPE_KEY = "jail.escape.fine-amount";
        public static final String EXTEND_ON_ESCAPE_KEY = "jail.escape.extend-time";
        public static final String RUN_COMMANDS_ON_ESCAPE_KEY = "jail.escape.run-commands";
        public static final String LOG_ESCAPE_ATTEMPTS_KEY = "jail.escape.log-attempts";
        public static final String BROADCAST_ESCAPE_ATTEMPTS_KEY = "jail.escape.broadcast-attempts";
        
        // Chaves de configuração de título de fuga
        public static final String ESCAPE_TITLE_ENABLED_KEY = "jail.escape.title.enabled";
        public static final String ESCAPE_TITLE_MAIN_KEY = "jail.escape.title.main";
        public static final String ESCAPE_TITLE_SUBTITLE_KEY = "jail.escape.title.subtitle";
        public static final String ESCAPE_TITLE_FADE_IN_KEY = "jail.escape.title.fade-in";
        public static final String ESCAPE_TITLE_STAY_KEY = "jail.escape.title.stay";
        public static final String ESCAPE_TITLE_FADE_OUT_KEY = "jail.escape.title.fade-out";
        
        // Chaves de configuração de fiança
        public static final String BAIL_ENABLED_KEY = "bail.enabled";
        public static final String ALLOW_SELF_BAIL_KEY = "bail.allow-self-bail";
        public static final String LOG_BAIL_PAYMENTS_KEY = "bail.log-bail-payments";
        
        // Chaves de configuração de restrições
        public static final String BLOCK_BREAK_KEY = "restrictions.block-break";
        public static final String BLOCK_PLACE_KEY = "restrictions.block-place";
        public static final String BLOCK_INTERACT_KEY = "restrictions.block-interact";
        public static final String PVP_ENABLED_KEY = "restrictions.pvp-enabled";
        public static final String BLOCK_CHAT_KEY = "restrictions.block-chat";
        public static final String BLOCK_SLEEP_KEY = "restrictions.block-sleep";
        public static final String BLOCK_ITEM_DROP_KEY = "restrictions.block-item-drop";
        public static final String BLOCK_ITEM_PICKUP_KEY = "restrictions.block-item-pickup";
        public static final String BLOCKED_COMMANDS_KEY = "restrictions.blocked-commands";
        public static final String ALLOWED_COMMANDS_KEY = "restrictions.allowed-commands";
        
        // Chaves de configuração de algemas
        public static final String SLOWNESS_LEVEL_KEY = "handcuffs.slowness-level";
        public static final String DARKNESS_LEVEL_KEY = "handcuffs.darkness-level";
        public static final String USE_MINING_FATIGUE_KEY = "handcuffs.use-mining-fatigue";
        public static final String MINING_FATIGUE_LEVEL_KEY = "handcuffs.mining-fatigue-level";
        
        // Chaves de configuração de áreas
        public static final String ESCAPE_DETECTION_KEY = "areas.escape-detection";
        public static final String ESCAPE_PUNISHMENT_COMMANDS_KEY = "areas.escape-punishment.commands";
        public static final String ESCAPE_PUNISHMENT_EXTRA_TIME_KEY = "areas.escape-punishment.extra-time";
        
        // Chaves de spawn de liberação
        public static final String RELEASE_SPAWN_WORLD_KEY = "release-spawn.world";
        public static final String RELEASE_SPAWN_X_KEY = "release-spawn.x";
        public static final String RELEASE_SPAWN_Y_KEY = "release-spawn.y";
        public static final String RELEASE_SPAWN_Z_KEY = "release-spawn.z";
        public static final String RELEASE_SPAWN_YAW_KEY = "release-spawn.yaw";
        public static final String RELEASE_SPAWN_PITCH_KEY = "release-spawn.pitch";
        
        // Valores padrão
        public static final String DEFAULT_LANGUAGE = "pt_BR";
        public static final String DEFAULT_STORAGE = "YAML";
        public static final String DEFAULT_TEMP_DURATION = "1h";
        public static final boolean DEFAULT_TELEPORT_BACK = true;
        public static final double DEFAULT_FINE_AMOUNT = 0.0;
        public static final String DEFAULT_EXTEND_TIME = "";
        public static final boolean DEFAULT_LOG_ESCAPE_ATTEMPTS = true;
        public static final boolean DEFAULT_BAIL_ENABLED = true;
        public static final boolean DEFAULT_ALLOW_SELF_BAIL = true;
        public static final boolean DEFAULT_LOG_BAIL_PAYMENTS = true;
        public static final boolean DEFAULT_ESCAPE_DETECTION = true;
        public static final int DEFAULT_SLOWNESS_LEVEL = 3;
        public static final int DEFAULT_DARKNESS_LEVEL = 1;
        public static final boolean DEFAULT_USE_MINING_FATIGUE = false;
        public static final int DEFAULT_MINING_FATIGUE_LEVEL = 2;
        public static final int DEFAULT_EXTRA_TIME = 0;
    }
    
    /**
     * Constantes de armazenamento
     */
    public static final class Storage {
        // Tipos de armazenamento
        public static final String YAML_TYPE = "YAML";
        public static final String SQLITE_TYPE = "SQLITE";
        
        // Nomes de arquivos
        public static final String JAILS_FILE = "jails.yml";
        public static final String PRISONERS_FILE = "prisoners.yml";
        public static final String FLAGS_FILE = "flags.yml";
        public static final String DATABASE_FILE = "devjails.db";
        
        // Seções YAML
        public static final String JAILS_SECTION = "jails";
        public static final String PRISONERS_SECTION = "prisoners";
        public static final String FLAGS_SECTION = "flags";
        
        // Campos de dados
        public static final String WORLD_FIELD = "world";
        public static final String X_FIELD = "x";
        public static final String Y_FIELD = "y";
        public static final String Z_FIELD = "z";
        public static final String YAW_FIELD = "yaw";
        public static final String PITCH_FIELD = "pitch";
        public static final String AREA_BINDING_FIELD = "areaBinding";
        public static final String AREA_REF_FIELD = "areaRef";
        public static final String JAIL_NAME_FIELD = "jailName";
        public static final String REASON_FIELD = "reason";
        public static final String STAFF_FIELD = "staff";
        public static final String JAIL_TIME_FIELD = "jailTime";
        public static final String RELEASE_TIME_FIELD = "releaseTime";
        public static final String BAIL_AMOUNT_FIELD = "bailAmount";
        public static final String BAIL_ENABLED_FIELD = "bailEnabled";
        public static final String HANDCUFFED_FIELD = "handcuffed";
        public static final String POST_RELEASE_SPAWN_CHOICE_FIELD = "postReleaseSpawnChoice";
        public static final String ORIGINAL_LOCATION_FIELD = "originalLocation";
        
        // Valores padrão
        public static final String DEFAULT_AREA_BINDING = "none";
        public static final String DEFAULT_SPAWN_CHOICE = "world_spawn";
        public static final boolean DEFAULT_BAIL_ENABLED = false;
        public static final boolean DEFAULT_HANDCUFFED = false;
    }
    
    /**
     * Constantes de API
     */
    public static final class API {
        public static final String VERSION = "1.0.0";
        public static final String PLUGIN_NAME = "DevJails";
    }
    
    /**
     * Constantes de comandos
     */
    public static final class Commands {
        // Subcomandos
        public static final String JAIL_COMMAND = "jail";
        public static final String TEMPJAIL_COMMAND = "tempjail";
        public static final String UNJAIL_COMMAND = "unjail";
        public static final String BAIL_COMMAND = "bail";
        public static final String LIST_COMMAND = "list";
        public static final String HELP_COMMAND = "help";
        public static final String WAND_COMMAND = "wand";
        public static final String SETSPAWN_COMMAND = "setspawn";
        public static final String FLAG_COMMAND = "flag";
        
        // Aliases para list
        public static final String JAILS_ALIAS = "jails";
        public static final String CADEIAS_ALIAS = "cadeias";
        public static final String PRISONERS_ALIAS = "prisoners";
        public static final String PRISIONEIROS_ALIAS = "prisioneiros";
        
        // Subcomandos de bail
        public static final String BAIL_SET_SUBCOMMAND = "set";
        public static final String BAIL_REMOVE_SUBCOMMAND = "remove";
        public static final String BAIL_GUI_SUBCOMMAND = "gui";
    }
    
    /**
     * Constantes de permissões
     */
    public static final class Permissions {
        public static final String BASE = "devjails";
        public static final String ADMIN = BASE + ".admin";
        public static final String JAIL = BASE + ".jail";
        public static final String TEMPJAIL = BASE + ".tempjail";
        public static final String UNJAIL = BASE + ".unjail";
        public static final String BAIL = BASE + ".bail";
        public static final String LIST = BASE + ".list";
        public static final String HELP = BASE + ".help";
        public static final String WAND = BASE + ".wand";
        public static final String SETSPAWN = BASE + ".setspawn";
        public static final String FLAG = BASE + ".flag";
        public static final String BYPASS = BASE + ".bypass";
        public static final String ESCAPE = BASE + ".escape";
    }
    
    /**
     * Constantes de mensagens
     */
    public static final class Messages {
        // Chaves de mensagens comuns
        public static final String NO_PERMISSION = "no_permission";
        public static final String PLAYER_NOT_FOUND = "player_not_found";
        public static final String PLAYER_OFFLINE = "player_offline";
        public static final String JAIL_NOT_FOUND = "jail_not_found";
        public static final String OPERATION_ERROR = "operation_error";
        public static final String INVALID_ARGS = "invalid_args";
        public static final String INVALID_NUMBER = "invalid_number";
        public static final String CANNOT_JAIL_SELF = "cannot_jail_self";
        public static final String PLAYER_ALREADY_JAILED = "player_already_jailed";
        public static final String PLAYER_NOT_JAILED = "player_not_jailed";
        
        // Arquivos de idioma
        public static final String PT_BR_FILE = "messages_pt_BR.yml";
        public static final String EN_US_FILE = "messages_en_US.yml";
    }
    
    /**
     * Constantes de validação
     */
    public static final class Validation {
        public static final int MIN_JAIL_NAME_LENGTH = 1;
        public static final int MAX_JAIL_NAME_LENGTH = 32;
        public static final int MIN_REASON_LENGTH = 1;
        public static final int MAX_REASON_LENGTH = 255;
        public static final double MIN_BAIL_AMOUNT = 0.01;
        public static final double MAX_BAIL_AMOUNT = 1000000.0;
        public static final int MIN_DURATION_SECONDS = 1;
        public static final int MAX_DURATION_SECONDS = Integer.MAX_VALUE;
    }
    
    /**
     * Constantes de efeitos de poção
     */
    public static final class PotionEffects {
        public static final int INFINITE_DURATION = Integer.MAX_VALUE;
        public static final int DEFAULT_AMPLIFIER = 0;
        public static final boolean SHOW_PARTICLES = false;
        public static final boolean SHOW_ICON = true;
    }
}