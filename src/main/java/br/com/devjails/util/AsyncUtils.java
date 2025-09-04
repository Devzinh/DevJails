package br.com.devjails.util;

import br.com.devjails.message.MessageService;
import org.bukkit.command.CommandSender;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Classe utilitária para operações assíncronas e tratamento de erros
 * Centraliza padrões comuns de async/await e error handling
 */
public class AsyncUtils {

  /**
   * Executa uma operação assíncrona com tratamento de erro padronizado
   */
  public static < T > CompletableFuture < T > executeAsync(Supplier < T > operation,
    String operationName,
    Logger logger) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return operation.get();
      } catch (Exception e) {
        logger.severe("Erro na operação " + operationName + ": " + e.getMessage());
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Executa uma operação assíncrona com callback de sucesso e tratamento de erro
   */
  public static < T > void executeAsyncWithCallback(Supplier < T > operation,
    Consumer < T > onSuccess,
    Consumer < Exception > onError,
    String operationName,
    Logger logger) {
    CompletableFuture.supplyAsync(() -> {
        try {
          return operation.get();
        } catch (Exception e) {
          logger.severe("Erro na operação " + operationName + ": " + e.getMessage());
          throw new RuntimeException(e);
        }
      }).thenAccept(onSuccess)
      .exceptionally(throwable -> {
        Exception cause = throwable.getCause() instanceof Exception ?
        (Exception) throwable.getCause() : new Exception(throwable);
        onError.accept(cause);
        return null;
      });
  }

  /**
   * Executa uma operação assíncrona com tratamento de erro para CommandSender
   */
  public static < T > void executeAsyncForSender(Supplier < T > operation,
    Consumer < T > onSuccess,
    CommandSender sender,
    MessageService messageService,
    String operationName,
    Logger logger) {
    executeAsyncWithCallback(
      operation,
      onSuccess,
      ex -> {
        messageService.sendMessage(sender, "operation_error");
        logger.severe("Erro na operação " + operationName + " para " + sender.getName() + ": " + ex.getMessage());
      },
      operationName,
      logger
    );
  }

  /**
   * Executa uma operação CompletableFuture com tratamento de erro padronizado
   */
  public static < T > void handleCompletableFuture(CompletableFuture < T > future,
    Consumer < T > onSuccess,
    String operationName,
    Logger logger) {
    future.thenAccept(onSuccess)
      .exceptionally(throwable -> {
        logger.severe("Erro na operação " + operationName + ": " + throwable.getMessage());
        throwable.printStackTrace();
        return null;
      });
  }

  /**
   * Executa uma operação CompletableFuture com tratamento de erro para CommandSender
   */
  public static < T > void handleCompletableFutureForSender(CompletableFuture < T > future,
    Consumer < T > onSuccess,
    CommandSender sender,
    MessageService messageService,
    String operationName,
    Logger logger) {
    future.thenAccept(onSuccess)
      .exceptionally(throwable -> {
        messageService.sendMessage(sender, "operation_error");
        logger.severe("Erro na operação " + operationName + " para " + sender.getName() + ": " + throwable.getMessage());
        return null;
      });
  }

  /**
   * Executa uma operação booleana assíncrona com callbacks específicos
   */
  public static void executeBooleanAsync(Supplier < Boolean > operation,
    Runnable onSuccess,
    Runnable onFailure,
    Consumer < Exception > onError,
    String operationName,
    Logger logger) {
    executeAsyncWithCallback(
      operation,
      success -> {
        if (success) {
          onSuccess.run();
        } else {
          onFailure.run();
        }
      },
      onError,
      operationName,
      logger
    );
  }

  /**
   * Executa uma operação booleana CompletableFuture com callbacks específicos
   */
  public static void handleBooleanFuture(CompletableFuture < Boolean > future,
    Runnable onSuccess,
    Runnable onFailure,
    String operationName,
    Logger logger) {
    future.thenAccept(success -> {
        if (success) {
          onSuccess.run();
        } else {
          onFailure.run();
        }
      })
      .exceptionally(throwable -> {
        logger.severe("Erro na operação " + operationName + ": " + throwable.getMessage());
        throwable.printStackTrace();
        return null;
      });
  }

  /**
   * Combina múltiplas operações assíncronas
   */
  public static < T, U, R > CompletableFuture < R > combineAsync(CompletableFuture < T > future1,
    CompletableFuture < U > future2,
    Function < T, Function < U, R >> combiner) {
    return future1.thenCompose(result1 ->
      future2.thenApply(result2 -> combiner.apply(result1).apply(result2))
    );
  }

  /**
   * Executa operações em sequência
   */
  public static < T, U > CompletableFuture < U > chainAsync(CompletableFuture < T > future,
    Function < T, CompletableFuture < U >> nextOperation) {
    return future.thenCompose(nextOperation);
  }

  /**
   * Executa uma operação com retry automático
   */
  public static < T > CompletableFuture < T > executeWithRetry(Supplier < CompletableFuture < T >> operation,
    int maxRetries,
    String operationName,
    Logger logger) {
    return executeWithRetryInternal(operation, maxRetries, 0, operationName, logger);
  }

  private static < T > CompletableFuture < T > executeWithRetryInternal(Supplier < CompletableFuture < T >> operation,
    int maxRetries,
    int currentAttempt,
    String operationName,
    Logger logger) {
    return operation.get().exceptionally(throwable -> {
      if (currentAttempt < maxRetries) {
        logger.warning("Tentativa " + (currentAttempt + 1) + " falhou para " + operationName + ", tentando novamente...");
        return executeWithRetryInternal(operation, maxRetries, currentAttempt + 1, operationName, logger).join();
      } else {
        logger.severe("Operação " + operationName + " falhou após " + maxRetries + " tentativas");
        throw new RuntimeException(throwable);
      }
    });
  }

  /**
   * Executa uma operação com timeout
   */
  public static < T > CompletableFuture < T > executeWithTimeout(CompletableFuture < T > future,
    long timeoutMillis,
    String operationName,
    Logger logger) {
    CompletableFuture < T > timeoutFuture = new CompletableFuture < > ();

    // Agendar timeout
    Tasks.syncDelayed(() -> {
      if (!timeoutFuture.isDone()) {
        logger.warning("Operação " + operationName + " expirou após " + timeoutMillis + "ms");
        timeoutFuture.completeExceptionally(new RuntimeException("Timeout na operação " + operationName));
      }
    }, timeoutMillis / 50); // Converter para ticks (50ms por tick)

    // Completar com o resultado da operação original
    future.whenComplete((result, throwable) -> {
      if (throwable != null) {
        timeoutFuture.completeExceptionally(throwable);
      } else {
        timeoutFuture.complete(result);
      }
    });

    return timeoutFuture;
  }
}