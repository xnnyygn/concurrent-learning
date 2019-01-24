package in.xnnyygn.concurrent.stm1;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class TransactionContext {

    private static final ThreadLocal<TransactionContext> localContext = ThreadLocal.withInitial(TransactionContext::new);
    private final Map<Reference<?>, Integer> readSet = new HashMap<>();
    private final Map<Reference<?>, Object> writeSet = new HashMap<>();

    public <T> T read(Reference<T> reference) {
        T value = reference.get();
        int version = reference.getVersion();
        Integer lastVersion = readSet.get(reference);
        // validate version
        if (lastVersion != null && lastVersion != version) {
            throw new RollbackException("reference changed when read again");
        }
        readSet.put(reference, version);
        return value;
    }

    public <T> void write(Reference<T> reference, T value) {
        int version = reference.getVersion();
        Integer lastVersion = readSet.get(reference);
        if (lastVersion == null) {
            // try to write without read
            readSet.put(reference, version);
            writeSet.put(reference, value);
            return;
        }
        if (lastVersion != version) {
            throw new RollbackException("reference changed before write");
        }
        writeSet.put(reference, value);
    }

    @SuppressWarnings("unchecked")
    private void commit() {
        synchronized (TransactionContext.class) {
            for (Reference<?> reference : readSet.keySet()) {
                if (reference.getVersion() != readSet.get(reference)) {
                    throw new RollbackException("reference changed before commit");
                }
            }
            for (Reference<?> reference : writeSet.keySet()) {
                ((Reference<Object>) reference).set(writeSet.get(reference), readSet.get(reference) + 1);
            }
        }
    }

    private void reset() {
        readSet.clear();
        writeSet.clear();
    }

    public static TransactionContext getLocal() {
        return localContext.get();
    }

    public static <T> T run(Callable<T> action) {
        TransactionContext context = localContext.get();
        T result;
        while (true) {
            try {
                result = action.call();
                context.commit();
                System.out.println(Thread.currentThread().getName() + " committed");
                return result;
            } catch (RollbackException e) {
                System.out.println(Thread.currentThread().getName() + " rollback, cause " + e.getMessage());
            } catch (Exception e) {
                // rollback
            } finally {
                context.reset();
            }
        }
    }

}
