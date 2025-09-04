package br.com.devjails.listener;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.message.MessageService;
import br.com.devjails.prisoner.PrisonerManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Classe base abstrata para listeners usando Template Method pattern
 * Consolida padrões comuns como verificação de prisioneiro, cooldowns e bypass de permissões
 */
public abstract class BaseListener implements Listener {
    
    protected final DevJailsPlugin plugin;
    protected final PrisonerManager prisonerManager;
    protected final MessageService messageService;
    protected final FileConfiguration config;
    
    // Sistema de cooldown para evitar spam de mensagens
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    
    protected BaseListener(DevJailsPlugin plugin) {
        this.plugin = plugin;
        this.prisonerManager = plugin.getPrisonerManager();
        this.messageService = plugin.getMessageService();
        this.config = plugin.getConfig();
    }
    
    /**
     * Template method para processar eventos relacionados a prisioneiros
     * Define o fluxo padrão de verificação e processamento
     */
    protected final boolean processPlayerEvent(Player player, Event event, String configKey, String messageKey) {
        return processPlayerEvent(player, event, configKey, messageKey, getCooldownDuration());
    }
    
    /**
     * Template method com cooldown customizado
     */
    protected final boolean processPlayerEvent(Player player, Event event, String configKey, String messageKey, long cooldownMs) {
        // 1. Verificar se o jogador é prisioneiro
        if (!isPrisonerCheck(player)) {
            return false;
        }
        
        // 2. Verificar bypass de permissão
        if (hasPermissionBypass(player)) {
            return false;
        }
        
        // 3. Verificar configuração
        if (!isRestrictionEnabled(configKey)) {
            return false;
        }
        
        // 4. Cancelar evento se for cancelável
        if (event instanceof Cancellable) {
            ((Cancellable) event).setCancelled(true);
        }
        
        // 5. Enviar mensagem com cooldown
        if (messageKey != null && !messageKey.isEmpty()) {
            sendCooldownMessage(player, messageKey, cooldownMs);
        }
        
        // 6. Executar lógica específica do listener
        onEventProcessed(player, event);
        
        return true;
    }
    
    /**
     * Verifica se o jogador é prisioneiro (método hook para customização)
     */
    protected boolean isPrisonerCheck(Player player) {
        return prisonerManager.isPrisonerSync(player.getUniqueId());
    }
    
    /**
     * Verifica se o jogador tem permissão de bypass
     */
    protected boolean hasPermissionBypass(Player player) {
        return player.hasPermission(getBypassPermission());
    }
    
    /**
     * Verifica se a restrição está habilitada na configuração
     */
    protected boolean isRestrictionEnabled(String configKey) {
        return config.getBoolean(configKey, true);
    }
    
    /**
     * Envia mensagem com sistema de cooldown usando método unificado
     */
    protected void sendCooldownMessage(Player player, String messageKey, long cooldownMs) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastMessage = cooldowns.get(playerId);
        
        if (lastMessage == null || (currentTime - lastMessage) >= cooldownMs) {
            messageService.sendUnifiedMessage(player, messageKey);
            cooldowns.put(playerId, currentTime);
        }
    }
    
    /**
     * Envia mensagem para o jogador usando o sistema unificado
     */
    protected void sendMessage(Player player, String messageKey) {
        messageService.sendUnifiedMessage(player, messageKey);
    }
    
    /**
     * Remove cooldown de um jogador (usado quando sai do servidor)
     */
    public void removePlayerCooldown(UUID playerId) {
        cooldowns.remove(playerId);
    }
    
    /**
     * Limpa todos os cooldowns
     */
    public void clearAllCooldowns() {
        cooldowns.clear();
    }
    
    // Métodos abstratos que devem ser implementados pelas subclasses
    
    /**
     * Retorna a permissão de bypass para este listener
     */
    protected abstract String getBypassPermission();
    
    /**
     * Retorna a duração padrão do cooldown em milissegundos
     */
    protected abstract long getCooldownDuration();
    
    /**
     * Método hook executado após o processamento do evento
     * Permite que subclasses implementem lógica específica
     */
    protected void onEventProcessed(Player player, Event event) {
        // Implementação padrão vazia - subclasses podem sobrescrever
    }
    
    // Métodos utilitários adicionais
    
    /**
     * Verifica se um jogador está em cooldown
     */
    protected boolean isInCooldown(UUID playerId, long cooldownMs) {
        Long lastTime = cooldowns.get(playerId);
        if (lastTime == null) {
            return false;
        }
        return (System.currentTimeMillis() - lastTime) < cooldownMs;
    }
    
    /**
     * Atualiza o cooldown de um jogador
     */
    protected void updateCooldown(UUID playerId) {
        cooldowns.put(playerId, System.currentTimeMillis());
    }
    
    /**
     * Remove o cooldown de um jogador (usado quando o jogador sai do servidor)
     */
    protected void removeCooldown(UUID playerId) {
        cooldowns.remove(playerId);
    }
    
    /**
     * Obtém o tempo restante de cooldown em milissegundos
     */
    protected long getRemainingCooldown(UUID playerId, long cooldownMs) {
        Long lastTime = cooldowns.get(playerId);
        if (lastTime == null) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - lastTime;
        return Math.max(0, cooldownMs - elapsed);
    }
}