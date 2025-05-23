package lol.jisz.astra.utils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility class for safely executing operations that might throw exceptions.
 * Provides methods for controlled exception handling and fallback mechanisms.
 */
public class AstraExecutor {

    /**
     * Functional interface for operations that return a value and might throw exceptions.
     *
     * @param <T> The type of the result
     */
    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    /**
     * Functional interface for operations that create and return a value without parameters.
     *
     * @param <T> The type of the result
     */
    @FunctionalInterface
    public interface ThrowingCreator<T> {
        T create() throws Exception;
    }

    /**
     * Executes a supplier that might throw an exception, with custom exception handling.
     *
     * @param supplier The operation to execute
     * @param exceptionHandler Handler for any exceptions that occur
     * @param fallback Supplier for a fallback value if an exception occurs
     * @param <T> The type of the result
     * @return The result of the supplier, or the fallback value if an exception occurs
     */
    public static <T> T ofUnchecked(ThrowingSupplier<T> supplier, 
                                   Consumer<Exception> exceptionHandler, 
                                   Supplier<T> fallback) {
        try {
            return supplier.get();
        } catch (Exception e) {
            exceptionHandler.accept(e);
            return fallback.get();
        }
    }

    /**
     * Executes a creator that might throw an exception, with custom exception handling.
     *
     * @param creator The operation to execute
     * @param exceptionHandler Handler for any exceptions that occur
     * @param fallback Supplier for a fallback value if an exception occurs
     * @param <T> The type of the result
     * @return The result of the creator, or the fallback value if an exception occurs
     */
    public static <T> T createUnchecked(ThrowingCreator<T> creator, 
                                      Consumer<Exception> exceptionHandler, 
                                      Supplier<T> fallback) {
        try {
            return creator.create();
        } catch (Exception e) {
            exceptionHandler.accept(e);
            return fallback.get();
        }
    }

    /**
     * Executes a supplier that might throw an exception, with default exception handling.
     *
     * @param supplier The operation to execute
     * @param <T> The type of the result
     * @return The result of the supplier, or null if an exception occurs
     */
    public static <T> T ofUnchecked(ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Executes a creator that might throw an exception, with default exception handling.
     *
     * @param creator The operation to execute
     * @param <T> The type of the result
     * @return The result of the creator, or null if an exception occurs
     */
    public static <T> T createUnchecked(ThrowingCreator<T> creator) {
        try {
            return creator.create();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}