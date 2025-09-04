package br.com.devjails.message;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import br.com.devjails.util.Tasks;
import br.com.devjails.util.Text;


public class MessageService {
    
    private final File dataFolder;
    private final Logger logger;
    private final Map<String, YamlConfiguration> localeConfigs;
    
    private String defaultLocale = "en_US";
    private String fallbackLocale = "en_US";
    private String configuredLocale;
    
    // Message delivery configuration
    private String deliveryType = "chat"; // "chat" or "title"
    private int titleFadeIn = 10;
    private int titleStay = 40;
    private int titleFadeOut = 10;
    
    public MessageService(File dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.localeConfigs = new HashMap<>();
    }
    
    
    public void initialize() {
        initialize(configuredLocale != null ? configuredLocale : "en_US");
    }
    
    /**
     * Initialize with configured language from config.yml
     * @param configuredLanguage The language configured in general.language
     */
    public void initialize(String configuredLanguage) {
        File messagesDir = new File(dataFolder, "messages");
        if (!messagesDir.exists()) {
            messagesDir.mkdirs();
        }
        
        // Store configured locale for reload persistence
        this.configuredLocale = configuredLanguage;
        
        // Set default locale from configuration, keep fallback as en_US
        if (configuredLanguage != null && !configuredLanguage.isEmpty()) {
            this.defaultLocale = configuredLanguage;
        } else {
            this.defaultLocale = "en_US";
        }
        this.fallbackLocale = "en_US"; // Always keep English as fallback
        
        loadDefaultLocales();
        loadCustomLocales();
        
        // Reduzir a verbosidade dos logs de inicialização
        logger.info("MessageService initialized with " + localeConfigs.size() + " languages loaded");
    }
    
    /**
     * Configure message delivery settings from config
     */
    public void configureMessages(org.bukkit.configuration.file.FileConfiguration config) {
        this.deliveryType = config.getString("messages.default-type", "chat");
        this.titleFadeIn = config.getInt("messages.title.fade-in", 10);
        this.titleStay = config.getInt("messages.title.stay", 40);
        this.titleFadeOut = config.getInt("messages.title.fade-out", 10);
        
        // Remover log detalhado de configuração de mensagens
        // logger.info("Configuração de mensagens: tipo=" + deliveryType + 
        //            ", title timing=" + titleFadeIn + "/" + titleStay + "/" + titleFadeOut);
    }
    
    public void setDefaultLocale(String locale) {
        this.defaultLocale = locale;
    }
    
    public void setFallbackLocale(String locale) {
        this.fallbackLocale = locale;
    }
    
    public String getMessage(String key, Object... placeholders) {
        return getLocalizedMessage(defaultLocale, key, placeholders);
    }
    
    public String getLocalizedMessage(String locale, String key, Object... placeholders) {
        String message = getRawMessage(locale, key);
        
        if (message == null) {
            return "§cMensagem não encontrada: " + key;
        }

        if (placeholders.length > 0) {
            message = Text.applyPlaceholders(message, placeholders);
        }
        
        return message;
    }
    
    public String getColorizedMessage(String key, Object... placeholders) {
        String message = getMessage(key, placeholders);
        return Text.colorizeToString(message);
    }
    
    public String getLocalizedColorizedMessage(String locale, String key, Object... placeholders) {
        String message = getLocalizedMessage(locale, key, placeholders);
        return Text.colorizeToString(message);
    }
    
    
    /**
     * @deprecated Use sendUnifiedMessage instead for centralized behavior
     */
    @Deprecated
    public void sendMessage(CommandSender sender, String key, Object... placeholders) {
        sendUnifiedMessage(sender, key, placeholders);
    }
    
    
    public void sendMessages(CommandSender sender, String... keys) {
        try {
            String locale = getPlayerLocale(sender);
            for (String key : keys) {
                String message = getLocalizedMessage(locale, key);
                String colorizedMessage = Text.colorizeToString(message);
                
                // Enviar mensagem diretamente sem conversão de codificação adicional
                sender.sendMessage(colorizedMessage);
            }
        } catch (Exception e) {
            logger.warning("Erro ao enviar mensagens: " + e.getMessage());
            sender.sendMessage("Erro ao carregar mensagens");
        }
    }
    
    
    public void reload() {
        localeConfigs.clear();
        initialize(configuredLocale != null ? configuredLocale : "en_US");
    }
    
    private String getRawMessage(String locale, String key) {
        // Tenta encontrar a mensagem no idioma solicitado
        YamlConfiguration config = localeConfigs.get(locale);
        if (config != null && config.contains(key)) {
            return config.getString(key);
        }

        // Se não for inglês, tenta em inglês primeiro como fallback universal
        if (!locale.equals(fallbackLocale)) {  // fallbackLocale é "en_US"
            config = localeConfigs.get(fallbackLocale);
            if (config != null && config.contains(key)) {
                return config.getString(key);
            }
        }
        
        // Se não encontrou no fallback, tenta no idioma padrão (en_US)
        if (!locale.equals(defaultLocale)) {
            config = localeConfigs.get(defaultLocale);
            if (config != null && config.contains(key)) {
                return config.getString(key);
            }
        }
        
        // Se tudo falhou, retorna null e o sistema vai mostrar mensagem não encontrada
        return null;
    }
    
    private String getPlayerLocale(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            String clientLocale = player.getLocale();
            
            // Converter o formato do cliente (ex: pt_pt, en_us) para o formato do sistema (pt_PT, en_US)
            if (clientLocale != null && !clientLocale.isEmpty()) {
                // Normalizar o formato (minúsculo para maísculo após _)
                String[] parts = clientLocale.split("_");
                if (parts.length == 2) {
                    clientLocale = parts[0].toLowerCase() + "_" + parts[1].toUpperCase();
                    
                    // Casos especiais
                    if (clientLocale.equals("pt_PT")) {
                        // Use PT-BR como alternativa para português europeu
                        clientLocale = "pt_BR";
                    } else if (clientLocale.equals("en_GB")) {
                        // Use EN-US como alternativa para inglês britânico
                        clientLocale = "en_US";
                    }
                    
                    // Verificar se temos esse idioma disponível
                    if (hasLocale(clientLocale)) {
                        return clientLocale;
                    }
                }
                
                // Caso não tenhamos o idioma específico, vamos tentar o idioma base (ex: 'pt' de 'pt_PT')
                if (parts.length >= 1) {
                    String baseLanguage = parts[0].toLowerCase();
                    
                    // Verificar idiomas base disponíveis
                    for (String availableLocale : getAvailableLocales()) {
                        if (availableLocale.startsWith(baseLanguage + "_")) {
                            return availableLocale;
                        }
                    }
                }
            }
            
            // Se não encontrarmos nada compatível, usar o padrão (en_US)
            return defaultLocale;
        }
        return defaultLocale;
    }
    
    
    private void loadDefaultLocales() {
        String[] locales = {
            "pt_BR", "en_US", "es_ES", "fr_FR", "de_DE", "ru_RU", "pl_PL"
        };
        
        // Reduzir a verbosidade dos logs de carregamento de idiomas
        logger.info("Loading language files...");
        
        for (String locale : locales) {
            try {
                InputStream resourceStream = getClass().getClassLoader()
                    .getResourceAsStream("messages/messages_" + locale + ".yml");
                
                if (resourceStream != null) {
                    // Adicionar tratamento explícito de codificação
                    byte[] bytes = resourceStream.readAllBytes();
                    String content = new String(bytes, StandardCharsets.UTF_8);
                    
                    // Remover qualquer BOM se ainda existir
                    if (content.startsWith("\uFEFF")) {
                        content = content.substring(1);
                    }
                    
                    // Usar um reader com a string já corrigida
                    YamlConfiguration config = new YamlConfiguration();
                    try (StringReader reader = new StringReader(content)) {
                        config.load(reader);
                    }
                    
                    localeConfigs.put(locale, config);
                    
                    // Remover log detalhado de cada idioma carregado
                    // logger.info("Idioma carregado: " + locale);
                    
                    // Salvar arquivo local se não existir
                    File localeFile = new File(dataFolder, "messages/messages_" + locale + ".yml");
                    if (!localeFile.exists()) {
                        try (FileOutputStream output = new FileOutputStream(localeFile)) {
                            output.write(content.getBytes(StandardCharsets.UTF_8));
                        }
                    }
                    
                    resourceStream.close();
                } else {
                    logger.warning("Language file not found: messages_" + locale + ".yml");
                }
            } catch (Exception e) {
                logger.warning("Error loading language " + locale + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    
    private void loadCustomLocales() {
        File messagesDir = new File(dataFolder, "messages");
        if (!messagesDir.exists()) {
            return;
        }
        
        File[] messageFiles = messagesDir.listFiles((dir, name) -> 
            name.startsWith("messages_") && name.endsWith(".yml")
        );
        
        if (messageFiles == null) {
            return;
        }
        
        for (File file : messageFiles) {
            try {
                String fileName = file.getName();
                String locale = fileName.substring(9, fileName.length() - 4);
                
                // Ler o arquivo com tratamento de codificação explícito
                byte[] bytes = Files.readAllBytes(file.toPath());
                String content = new String(bytes, StandardCharsets.UTF_8);
                
                // Remover BOM se existir
                if (content.startsWith("\uFEFF")) {
                    content = content.substring(1);
                }
                
                // Usar reader com a string já corrigida
                YamlConfiguration config = new YamlConfiguration();
                try (StringReader reader = new StringReader(content)) {
                    config.load(reader);
                }

                YamlConfiguration existing = localeConfigs.get(locale);
                if (existing != null) {
                    for (String key : config.getKeys(true)) {
                        existing.set(key, config.get(key));
                    }
                } else {
                    localeConfigs.put(locale, config);
                }
                
            } catch (Exception e) {
                logger.warning("Error loading message file " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    public String[] getAvailableLocales() {
        return localeConfigs.keySet().toArray(new String[0]);
    }
    
    /**
     * Retorna o código de idioma normalizado para o formato do sistema
     * @param player Jogador para obter o idioma
     * @return Código de idioma normalizado (ex: pt_BR, en_US)
     */
    public String getPlayerLanguage(Player player) {
        if (player == null) {
            return defaultLocale;
        }
        return getPlayerLocale(player);
    }
    
    public boolean hasLocale(String locale) {
        return localeConfigs.containsKey(locale);
    }
    
    /**
     * Método unificado para envio de mensagens que decide entre chat ou title baseado em configuração
     */
    public void sendUnifiedMessage(CommandSender sender, String key, Object... placeholders) {
        try {
            String locale = getPlayerLocale(sender);
            String message = getLocalizedMessage(locale, key, placeholders);
            String colorizedMessage = Text.colorizeToString(message);
            
            if (sender instanceof Player) {
                Player player = (Player) sender;
                // Use configured delivery type
                if ("title".equalsIgnoreCase(deliveryType)) {
                    player.sendTitle("", colorizedMessage, titleFadeIn, titleStay, titleFadeOut);
                } else {
                    player.sendMessage(colorizedMessage);
                }
            } else {
                sender.sendMessage(colorizedMessage);
            }
        } catch (Exception e) {
            logger.warning("Error sending unified message '" + key + "': " + e.getMessage());
            sender.sendMessage("Error loading message: " + key);
        }
    }
    
    /**
     * Método para mensagens compostas como notificação de prisão
     */
    public void sendCompositeMessage(Player player, String key, Object... placeholders) {
        sendUnifiedMessage(player, key, placeholders);
    }
    
    /**
     * Método centralizado para broadcasts com permissão
     */
    public void broadcastToPermission(String permission, String key, Object... placeholders) {
        broadcastToPermission(permission, null, key, placeholders);
    }
    
    /**
     * Método centralizado para broadcasts com permissão e exclusão de recipients
     */
    public void broadcastToPermission(String permission, java.util.Set<java.util.UUID> exclude, String key, Object... placeholders) {
        org.bukkit.Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.hasPermission(permission) && (exclude == null || !exclude.contains(player.getUniqueId()))) {
                String localizedMessage = getLocalizedColorizedMessage(getPlayerLanguage(player), key, placeholders);
                player.sendMessage(localizedMessage);
            }
        });
    }
    
    /**
     * Método específico para notificações de fuga consolidadas
     * Envia title/subtitle ao invés de broadcast para evitar spam
     */
    public void sendEscapeNotification(Player player, boolean teleported, double fineAmount, String timeExtended) {
        String playerLanguage = getPlayerLanguage(player);
        
        // Título principal
        String title = getLocalizedMessage(playerLanguage, "escape_title");
        
        // Construir subtitle com informações relevantes
        StringBuilder subtitleBuilder = new StringBuilder();
        
        if (teleported) {
            subtitleBuilder.append(getLocalizedMessage(playerLanguage, "escape_teleport_part"));
        }
        
        if (fineAmount > 0) {
            if (subtitleBuilder.length() > 0) subtitleBuilder.append(" ");
            subtitleBuilder.append(getLocalizedMessage(playerLanguage, "escape_fine_part", "{amount}", String.valueOf(fineAmount)));
        }
        
        if (timeExtended != null) {
            if (subtitleBuilder.length() > 0) subtitleBuilder.append(" ");
            subtitleBuilder.append(getLocalizedMessage(playerLanguage, "escape_time_extended_part", "{time}", timeExtended));
        }
        
        String subtitle = subtitleBuilder.toString();
        
        // Enviar title/subtitle diretamente
        Tasks.sync(() -> {
            player.sendTitle(title, subtitle, 10, 60, 20);
        });
    }
    
    /**
     * Método específico para notificações de prisão consolidadas
     */
    public void sendJailNotification(Player player, String jail, String reason, String staff, String duration) {
        sendUnifiedMessage(player, "jail_notification_full",
            "{jail}", jail,
            "{reason}", reason,
            "{staff}", staff,
            "{duration}", duration);
    }
    
}