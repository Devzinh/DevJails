package br.com.devjails.gui;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.message.MessageService;
import br.com.devjails.util.Tasks;

/**
 * Classe base abstrata para GUIs com paginação e navegação
 * Implementa padrões comuns de event handling e lifecycle management
 */
public abstract class BaseGUI<T> implements Listener {
    
    protected final DevJailsPlugin plugin;
    protected final MessageService messageService;
    protected final Player viewer;
    protected final List<T> items;
    
    protected Inventory inventory;
    protected int currentPage = 1;
    protected int totalPages = 1;
    
    // Configurações abstratas que devem ser definidas pelas subclasses
    protected abstract int getItemsPerPage();
    protected abstract int getGuiSize();
    protected abstract String getGuiTitle();
    protected abstract String getNoItemsMessage();
    
    // Slots de navegação abstratos
    protected abstract int getPreviousPageSlot();
    protected abstract int getNextPageSlot();
    protected abstract int getCloseSlot();
    protected abstract int getPageInfoSlot();
    
    public BaseGUI(DevJailsPlugin plugin, Player viewer, List<T> items) {
        this.plugin = plugin;
        this.messageService = plugin.getMessageService();
        this.viewer = viewer;
        this.items = items;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Abre a GUI na página especificada
     */
    public void open(int page) {
        this.currentPage = page;
        Tasks.async(() -> {
            try {
                loadData();
                calculatePagination();
                
                Tasks.sync(() -> {
                    createInventory();
                    viewer.openInventory(inventory);
                });
                
            } catch (Exception ex) {
                plugin.getLogger().severe("Erro ao carregar dados para GUI: " + ex.getMessage());
                Tasks.sync(() -> {
                    messageService.sendMessage(viewer, "storage_error");
                });
            }
        });
    }
    
    /**
     * Carrega os dados necessários para a GUI
     * Deve ser implementado pelas subclasses
     */
    protected abstract void loadData();
    
    /**
     * Calcula a paginação baseada no número de itens
     */
    protected void calculatePagination() {
        totalPages = Math.max(1, (int) Math.ceil((double) items.size() / getItemsPerPage()));
        
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
    }
    
    /**
     * Cria o inventory da GUI
     */
    protected void createInventory() {
        this.inventory = Bukkit.createInventory(viewer, getGuiSize(), getGuiTitle());
        inventory.clear();
        
        if (items.isEmpty()) {
            addNoItemsIndicator();
            return;
        }
        
        addItems();
        addNavigationItems();
        addBorders();
        addCustomItems();
    }
    
    /**
     * Adiciona indicador quando não há itens
     */
    protected void addNoItemsIndicator() {
        ItemStack noItemsItem = new ItemStack(Material.BARRIER);
        ItemMeta meta = noItemsItem.getItemMeta();
        meta.setDisplayName(getNoItemsMessage());
        noItemsItem.setItemMeta(meta);
        
        // Posiciona no centro da GUI
        int centerSlot = (getGuiSize() / 2) - 5;
        inventory.setItem(centerSlot, noItemsItem);
    }
    
    /**
     * Adiciona os itens da página atual
     */
    protected void addItems() {
        int startIndex = (currentPage - 1) * getItemsPerPage();
        int endIndex = Math.min(startIndex + getItemsPerPage(), items.size());
        
        int[] contentSlots = getContentSlots();
        
        for (int i = startIndex; i < endIndex; i++) {
            T item = items.get(i);
            int slotIndex = i - startIndex;
            
            if (slotIndex < contentSlots.length) {
                ItemStack itemStack = createItemStack(item);
                inventory.setItem(contentSlots[slotIndex], itemStack);
            }
        }
    }
    
    /**
     * Retorna os slots onde os itens de conteúdo devem ser colocados
     * Deve ser implementado pelas subclasses
     */
    protected abstract int[] getContentSlots();
    
    /**
     * Cria um ItemStack para um item específico
     * Deve ser implementado pelas subclasses
     */
    protected abstract ItemStack createItemStack(T item);
    
    /**
     * Adiciona itens de navegação
     */
    protected void addNavigationItems() {
        // Página anterior
        if (currentPage > 1) {
            ItemStack prevItem = new ItemStack(Material.ARROW);
            ItemMeta meta = prevItem.getItemMeta();
            meta.setDisplayName(messageService.getMessage("gui_previous_page"));
            prevItem.setItemMeta(meta);
            inventory.setItem(getPreviousPageSlot(), prevItem);
        }
        
        // Próxima página
        if (currentPage < totalPages) {
            ItemStack nextItem = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextItem.getItemMeta();
            nextMeta.setDisplayName(messageService.getMessage("gui_next_page"));
            nextItem.setItemMeta(nextMeta);
            inventory.setItem(getNextPageSlot(), nextItem);
        }
        
        // Informações da página
        if (getPageInfoSlot() >= 0) {
            ItemStack pageInfo = new ItemStack(Material.BOOK);
            ItemMeta meta = pageInfo.getItemMeta();
            meta.setDisplayName(messageService.getMessage("gui_current_page")
                .replace("{page}", String.valueOf(currentPage))
                .replace("{total}", String.valueOf(totalPages)));
            pageInfo.setItemMeta(meta);
            inventory.setItem(getPageInfoSlot(), pageInfo);
        }
        
        // Botão fechar
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(messageService.getMessage("gui_close"));
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(getCloseSlot(), closeItem);
    }
    
    /**
     * Adiciona bordas decorativas (implementação padrão)
     * Pode ser sobrescrita pelas subclasses
     */
    protected void addBorders() {
        // Implementação padrão vazia - subclasses podem sobrescrever
    }
    
    /**
     * Adiciona itens customizados específicos da GUI
     * Pode ser sobrescrita pelas subclasses
     */
    protected void addCustomItems() {
        // Implementação padrão vazia - subclasses podem sobrescrever
    }
    
    /**
     * Event handler para cliques no inventory
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) {
            return;
        }
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        if (!player.equals(viewer)) {
            return;
        }
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        
        int slot = event.getSlot();
        
        // Navegação - Página anterior
        if (slot == getPreviousPageSlot() && currentPage > 1) {
            open(currentPage - 1);
            return;
        }
        
        // Navegação - Próxima página
        if (slot == getNextPageSlot() && currentPage < totalPages) {
            open(currentPage + 1);
            return;
        }
        
        // Fechar GUI
        if (slot == getCloseSlot()) {
            player.closeInventory();
            return;
        }
        
        // Delegar clique em item de conteúdo para subclasse
        handleContentClick(event, slot);
    }
    
    /**
     * Manipula cliques em itens de conteúdo
     * Deve ser implementado pelas subclasses
     */
    protected abstract void handleContentClick(InventoryClickEvent event, int slot);
    
    /**
     * Event handler para fechamento do inventory
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory)) {
            // Desregistra os event handlers
            InventoryClickEvent.getHandlerList().unregister(this);
            InventoryCloseEvent.getHandlerList().unregister(this);
        }
    }
    
    /**
     * Utilitário para encontrar o índice do item baseado no slot clicado
     */
    protected int getItemIndexFromSlot(int slot) {
        int[] contentSlots = getContentSlots();
        
        for (int i = 0; i < contentSlots.length; i++) {
            if (contentSlots[i] == slot) {
                return ((currentPage - 1) * getItemsPerPage()) + i;
            }
        }
        
        return -1;
    }
    
    /**
     * Verifica se um slot é um slot de conteúdo
     */
    protected boolean isContentSlot(int slot) {
        int[] contentSlots = getContentSlots();
        
        for (int contentSlot : contentSlots) {
            if (contentSlot == slot) {
                return true;
            }
        }
        
        return false;
    }
    
    // Getters
    public int getCurrentPage() {
        return currentPage;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public List<T> getItems() {
        return items;
    }
    
    public Player getViewer() {
        return viewer;
    }
}