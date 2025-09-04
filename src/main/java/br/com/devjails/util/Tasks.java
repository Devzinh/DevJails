package br.com.devjails.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.CompletableFuture;

/**
 * UtilitÃ¡rio para gerenciar tarefas assÃ­ncronas e sÃ­ncronas
 */
public class Tasks {
    
    private static Plugin plugin;
    
    /**
     * Inicializa o utilitÃ¡rio com a instÃ¢ncia do plugin
     */
    public static void init(Plugin plugin) {
        Tasks.plugin = plugin;
    }
    
    /**
     * Executa uma tarefa sÃ­ncrona (main thread)
     */
    public static BukkitTask sync(Runnable task) {
        if (plugin == null || !plugin.isEnabled()) {
            return null;
        }
        return Bukkit.getScheduler().runTask(plugin, task);
    }
    
    /**
     * Executa uma tarefa sÃ­ncrona com delay
     */
    public static BukkitTask syncDelayed(Runnable task, long delay) {
        return Bukkit.getScheduler().runTaskLater(plugin, task, delay);
    }
    
    /**
     * Executa uma tarefa sÃ­ncrona repetitiva
     */
    public static BukkitTask syncRepeating(Runnable task, long delay, long period) {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
    }
    
    /**
     * Executa uma tarefa assÃ­ncrona
     */
    public static BukkitTask async(Runnable task) {
        if (plugin == null || !plugin.isEnabled()) {
            return null;
        }
        return Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }
    
    /**
     * Executa uma tarefa assÃ­ncrona com delay
     */
    public static BukkitTask asyncDelayed(Runnable task, long delay) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
    }
    
    /**
     * Executa uma tarefa assÃ­ncrona repetitiva
     */
    public static BukkitTask asyncRepeating(Runnable task, long delay, long period) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period);
    }
    
    /**
     * Cancela uma tarefa
     */
    public static void cancel(BukkitTask task) {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }
    
    /**
     * Cancela uma tarefa por ID
     */
    public static void cancel(int taskId) {
        Bukkit.getScheduler().cancelTask(taskId);
    }
    
    /**
     * Executa uma operaÃ§Ã£o de I/O assÃ­ncrona e depois executa o callback na main thread
     */
    public static CompletableFuture<Void> asyncThenSync(Runnable asyncTask, Runnable syncCallback) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        // Se o plugin está desabilitado, executa diretamente de forma síncrona
        if (plugin == null || !plugin.isEnabled()) {
            try {
                asyncTask.run();
                if (syncCallback != null) {
                    syncCallback.run();
                }
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
            return future;
        }
        
        BukkitTask task = async(() -> {
            try {
                asyncTask.run();
            } finally {
                if (syncCallback != null) {
                    sync(() -> {
                        syncCallback.run();
                        future.complete(null);
                    });
                } else {
                    future.complete(null);
                }
            }
        });
        
        // Se não conseguiu criar a tarefa, executa diretamente
        if (task == null) {
            try {
                asyncTask.run();
                if (syncCallback != null) {
                    syncCallback.run();
                }
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }
        
        return future;
    }
    
    /**
     * Executa uma operaÃ§Ã£o de I/O assÃ­ncrona e depois executa o callback na main thread com resultado
     */
    public static <T> CompletableFuture<T> asyncThenSync(AsyncSupplier<T> asyncTask, AsyncCallback<T> syncCallback) {
        CompletableFuture<T> future = new CompletableFuture<>();
        async(() -> {
            T result = null;
            Exception error = null;
            
            try {
                result = asyncTask.get();
            } catch (Exception e) {
                error = e;
            }
            
            if (syncCallback != null) {
                final T finalResult = result;
                final Exception finalError = error;
                sync(() -> {
                    if (finalError != null) {
                        syncCallback.onError(finalError);
                        future.completeExceptionally(finalError);
                    } else {
                        syncCallback.onSuccess(finalResult);
                        future.complete(finalResult);
                    }
                });
            } else {
                if (error != null) {
                    future.completeExceptionally(error);
                } else {
                    future.complete(result);
                }
            }
        });
        return future;
    }
    
    /**
     * Interface para operaÃ§Ãµes assÃ­ncronas que retornam um valor
     */
    @FunctionalInterface
    public interface AsyncSupplier<T> {
        T get() throws Exception;
    }
    
    
    public interface AsyncCallback<T> {
        void onSuccess(T result);
        
        default void onError(Exception error) {
            error.printStackTrace();
        }
    }
}