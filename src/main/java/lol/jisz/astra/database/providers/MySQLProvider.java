package lol.jisz.astra.database.providers;

import lol.jisz.astra.database.AstraDatabase;
import lol.jisz.astra.database.interfaces.StorageObject;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class MySQLProvider extends AstraDatabase {

    @Override
    public void initialize() throws Exception {

    }

    @Override
    public void close() {

    }

    @Override
    public String getType() {
        return "";
    }

    @Override
    public <T extends StorageObject> CompletableFuture<Optional<T>> findById(Class<T> clazz, String id) {
        return null;
    }

    @Override
    public <T extends StorageObject> Optional<T> findByIdSync(Class<T> clazz, String id) {
        return Optional.empty();
    }

    @Override
    public <T extends StorageObject> CompletableFuture<Set<T>> findAll(Class<T> clazz) {
        return null;
    }

    @Override
    public <T extends StorageObject> Set<T> findAllSync(Class<T> clazz) {
        return Set.of();
    }

    @Override
    public <T extends StorageObject> CompletableFuture<Void> save(T object) {
        return null;
    }

    @Override
    public <T extends StorageObject> void saveSync(T object) {

    }

    @Override
    public <T extends StorageObject> CompletableFuture<Void> delete(Class<T> clazz, String id) {
        return null;
    }

    @Override
    public <T extends StorageObject> void deleteSync(Class<T> clazz, String id) {

    }
}