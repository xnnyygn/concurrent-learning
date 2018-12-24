package in.xnnyygn.concurrent.exchange;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("Duplicates")
public class JucExchanger<V> {

    /**
     * The byte distance (as a shift value) between any two used slots
     * in the arena.  1 << ASHIFT should be at least cacheline size.
     */
    private static final int ASHIFT = 7;

    /**
     * The maximum supported arena index. The maximum allocatable
     * arena size is MMASK + 1. Must be a power of two minus one, less
     * than (1<<(31-ASHIFT)). The cap of 255 (0xff) more than suffices
     * for the expected scaling limits of the main algorithms.
     */
    private static final int MMASK = 0xff;

    /**
     * Unit for sequence/version bits of bound field. Each successful
     * change to the bound also adds SEQ.
     */
    private static final int SEQ = MMASK + 1;

    /**
     * The number of CPUs, for sizing and spin control
     */
    private static final int NCPU = Runtime.getRuntime().availableProcessors();

    /**
     * The maximum slot index of the arena: The number of slots that
     * can in principle hold all threads without contention, or at
     * most the maximum indexable value.
     */
    private static final int FULL = (NCPU >= (MMASK << 1)) ? MMASK : NCPU >>> 1;

    /**
     * The bound for spins while waiting for a match. The actual
     * number of iterations will on average be about twice this value
     * due to randomization. Note: Spinning is disabled when NCPU==1.
     */
    private static final int SPINS = 1 << 10;

    /**
     * Value representing null arguments/returns from public
     * methods. Needed because the API originally didn't disallow null
     * arguments, which it should have.
     */
    private static final Object NULL_ITEM = new Object();

    /**
     * Sentinel value returned by internal exchange methods upon
     * timeout, to avoid need for separate timed versions of these
     * methods.
     */
    private static final Object TIMED_OUT = new Object();

    /**
     * Nodes hold partially exchanged data, plus other per-thread
     * bookkeeping. Padded via @sun.misc.Contended to reduce memory
     * contention.
     */
    @sun.misc.Contended
    static final class Node {
        int index;              // Arena index
        int bound;              // Last recorded value of Exchanger.bound
        int collides;           // Number of CAS failures at current bound
        int hash;               // Pseudo-random for spins
        Object item;            // This thread's current item
        volatile Object match;  // Item provided by releasing thread
        volatile Thread parked; // Set to this thread when parked, else null
    }

    /**
     * The corresponding thread local class
     */
    static final class Participant extends ThreadLocal<Node> {
        public Node initialValue() {
            return new Node();
        }
    }

    /**
     * Per-thread state
     */
    private final Participant participant;

    /**
     * Elimination array; null until enabled (within slotExchange).
     * Element accesses use emulation of volatile gets and CAS.
     */
    private volatile Node[] arena;

    /**
     * Slot used until contention detected.
     */
    private volatile Node slot;

    /**
     * The index of the largest valid arena position, OR'ed with SEQ
     * number in high bits, incremented on each update.  The initial
     * update from 0 to SEQ is used to ensure that the arena array is
     * constructed only once.
     */
    private volatile int bound;

    /**
     * Exchange function when arenas enabled. See above for explanation.
     *
     * @param item  the (non-null) item to exchange
     * @param timed true if the wait is timed
     * @param ns    if timed, the maximum wait time, else 0L
     * @return the other thread's item; or null if interrupted; or
     * TIMED_OUT if timed and timed out
     */
    public Object arenaExchange(Object item, boolean timed, long ns) {
        Node[] a = arena;
        Node p = participant.get();
        int b, max, c;
        long rawArrayOffset;                       // j is raw array offset
        final Thread me = Thread.currentThread();
        for (int i = p.index; ; ) {                      // access slot at i
            Node q = (Node) U.getObjectVolatile(a, rawArrayOffset = (i << ASHIFT) + ABASE);
            // if node present and try to swap
            if (q != null && U.compareAndSwapObject(a, rawArrayOffset, q, null)) {
                Object v = q.item;                     // release
                q.match = item;
                Thread w = q.parked;
                if (w != null)
                    U.unpark(w);
                return v;
            }

            // if index is legal and node not present
            if (i <= (max = (b = bound) & MMASK) && q == null) {
                p.item = item;                         // offer

                // try to swap it
                if (U.compareAndSwapObject(a, rawArrayOffset, null, p)) {
                    long end = (timed && max == 0) ? System.nanoTime() + ns : 0L;

                    // wait
                    for (int h = p.hash, spins = SPINS; ; ) {

                        // got match
                        Object v = p.match;
                        if (v != null) {
                            U.putOrderedObject(p, MATCH, null);
                            p.item = null;             // clear for next use
                            p.hash = h;
                            return v;
                        }

                        if (spins > 0) {
                            h ^= h << 1;
                            h ^= h >>> 3;
                            h ^= h << 10; // xorshift
                            if (h == 0)                // initialize hash
                                h = SPINS | (int) me.getId();
                            else if (h < 0 &&          // approx 50% true
                                    (--spins & ((SPINS >>> 1) - 1)) == 0) {
                                Thread.yield();        // two yields per wait
                            }
                        } else if (U.getObjectVolatile(a, rawArrayOffset) != p) {
                            spins = SPINS;       // releaser hasn't set match yet
                        } else if (!me.isInterrupted() && max == 0 && // spin -> park only if max is 0
                                (!timed || (ns = end - System.nanoTime()) > 0L)) {
                            // park
                            U.putObject(me, BLOCKER, this); // emulate LockSupport
                            p.parked = me;              // minimize window
                            if (U.getObjectVolatile(a, rawArrayOffset) == p) { // node not changed
                                U.park(false, ns);
                            }
                            p.parked = null;
                            U.putObject(me, BLOCKER, null);
                        } else if (U.getObjectVolatile(a, rawArrayOffset) == p && // no pair thread, try to swap
                                U.compareAndSwapObject(a, rawArrayOffset, p, null)) {

                            if (max != 0) {                // try to shrink
                                U.compareAndSwapInt(this, BOUND, b, b + SEQ - 1);
                            }
                            p.item = null;
                            p.hash = h;
                            i = (p.index >>>= 1);        // descend, try another slot
                            if (Thread.interrupted()) {
                                return null;
                            }
                            if (timed && max == 0 && ns <= 0L) {
                                return TIMED_OUT;
                            }
                            break;                     // expired; restart
                        }
                    }
                } else { // node = null, swap failed
                    p.item = null;                     // clear offer
                }
            } else {
                if (p.bound != b) {                    // stale; reset
                    p.bound = b;
                    p.collides = 0;
                    i = (i != max || max == 0) ? max : (max - 1); // if i == max && max != 0 then i = max - 1 else i = max
                } else if ((c = p.collides) < max ||
                        max == FULL ||
                        !U.compareAndSwapInt(this, BOUND, b, b + SEQ + 1)) {

                    // c < max
                    // c >= max, max == full
                    // c >= max, max != full, failed to increase; 1. increase, goto else 2. failed to increase

                    // increase collides
                    // goto next
                    p.collides = c + 1;
                    i = (i == 0) ? max : i - 1;          // cyclically traverse
                } else {
                    i = max + 1;                         // grow, to condition above
                }

                p.index = i;
            }
        }
    }

    /**
     * Exchange function used until arenas enabled. See above for explanation.
     *
     * @param item  the item to exchange
     * @param timed true if the wait is timed
     * @param ns    if timed, the maximum wait time, else 0L
     * @return the other thread's item; or null if either the arena
     * was enabled or the thread was interrupted before completion; or
     * TIMED_OUT if timed and timed out
     */
    private Object slotExchange(Object item, boolean timed, long ns) {
        Node p = participant.get();
        Thread t = Thread.currentThread();
        if (t.isInterrupted()) // preserve interrupt status so caller can recheck
            return null;

        for (Node q; ; ) {
            if ((q = slot) != null) {
                if (U.compareAndSwapObject(this, SLOT, q, null)) {
                    Object v = q.item;
                    q.match = item;
                    Thread w = q.parked;
                    if (w != null)
                        U.unpark(w);
                    return v;
                }
                // create arena on contention, but continue until slot null
                if (NCPU > 1 && bound == 0 &&
                        U.compareAndSwapInt(this, BOUND, 0, SEQ))
                    arena = new Node[(FULL + 2) << ASHIFT];
            } else if (arena != null)
                return null; // caller must reroute to arenaExchange
            else {
                p.item = item;
                if (U.compareAndSwapObject(this, SLOT, null, p))
                    break;
                p.item = null;
            }
        }

        // await release
        int h = p.hash;
        long end = timed ? System.nanoTime() + ns : 0L;
        int spins = (NCPU > 1) ? SPINS : 1;
        Object v;
        while ((v = p.match) == null) {
            if (spins > 0) {
                h ^= h << 1;
                h ^= h >>> 3;
                h ^= h << 10;
                if (h == 0)
                    h = SPINS | (int) t.getId();
                else if (h < 0 && (--spins & ((SPINS >>> 1) - 1)) == 0)
                    Thread.yield();
            } else if (slot != p)
                spins = SPINS;
            else if (!t.isInterrupted() && arena == null &&
                    (!timed || (ns = end - System.nanoTime()) > 0L)) {
                U.putObject(t, BLOCKER, this);
                p.parked = t;
                if (slot == p)
                    U.park(false, ns);
                p.parked = null;
                U.putObject(t, BLOCKER, null);
            } else if (U.compareAndSwapObject(this, SLOT, p, null)) {
                v = timed && ns <= 0L && !t.isInterrupted() ? TIMED_OUT : null;
                break;
            }
        }
        U.putOrderedObject(p, MATCH, null);
        p.item = null;
        p.hash = h;
        return v;
    }

    /**
     * Creates a new Exchanger.
     */
    public JucExchanger() {
        participant = new Participant();
    }

    /**
     * Waits for another thread to arrive at this exchange point (unless
     * the current thread is {@linkplain Thread#interrupt interrupted}),
     * and then transfers the given object to it, receiving its object
     * in return.
     *
     * <p>If another thread is already waiting at the exchange point then
     * it is resumed for thread scheduling purposes and receives the object
     * passed in by the current thread.  The current thread returns immediately,
     * receiving the object passed to the exchange by that other thread.
     *
     * <p>If no other thread is already waiting at the exchange then the
     * current thread is disabled for thread scheduling purposes and lies
     * dormant until one of two things happens:
     * <ul>
     * <li>Some other thread enters the exchange; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread.
     * </ul>
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * for the exchange,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * @param x the object to exchange
     * @return the object provided by the other thread
     * @throws InterruptedException if the current thread was
     *                              interrupted while waiting
     */
    @SuppressWarnings("unchecked")
    public V exchange(V x) throws InterruptedException {
        Object v;
        Object item = (x == null) ? NULL_ITEM : x; // translate null args
        if ((arena != null ||
                (v = slotExchange(item, false, 0L)) == null) &&
                ((Thread.interrupted() || // disambiguates null return
                        (v = arenaExchange(item, false, 0L)) == null)))
            throw new InterruptedException();
        return (v == NULL_ITEM) ? null : (V) v;
    }

    /**
     * Waits for another thread to arrive at this exchange point (unless
     * the current thread is {@linkplain Thread#interrupt interrupted} or
     * the specified waiting time elapses), and then transfers the given
     * object to it, receiving its object in return.
     *
     * <p>If another thread is already waiting at the exchange point then
     * it is resumed for thread scheduling purposes and receives the object
     * passed in by the current thread.  The current thread returns immediately,
     * receiving the object passed to the exchange by that other thread.
     *
     * <p>If no other thread is already waiting at the exchange then the
     * current thread is disabled for thread scheduling purposes and lies
     * dormant until one of three things happens:
     * <ul>
     * <li>Some other thread enters the exchange; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>The specified waiting time elapses.
     * </ul>
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * for the exchange,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then {@link
     * TimeoutException} is thrown.  If the time is less than or equal
     * to zero, the method will not wait at all.
     *
     * @param x       the object to exchange
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the {@code timeout} argument
     * @return the object provided by the other thread
     * @throws InterruptedException if the current thread was
     *                              interrupted while waiting
     * @throws TimeoutException     if the specified waiting time elapses
     *                              before another thread enters the exchange
     */
    @SuppressWarnings("unchecked")
    public V exchange(V x, long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException {
        Object v;
        Object item = (x == null) ? NULL_ITEM : x;
        long ns = unit.toNanos(timeout);
        if ((arena != null ||
                (v = slotExchange(item, true, ns)) == null) &&
                ((Thread.interrupted() ||
                        (v = arenaExchange(item, true, ns)) == null)))
            throw new InterruptedException();
        if (v == TIMED_OUT)
            throw new TimeoutException();
        return (v == NULL_ITEM) ? null : (V) v;
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe U;
    private static final long BOUND;
    private static final long SLOT;
    private static final long MATCH;
    private static final long BLOCKER;
    private static final int ABASE;

    static {
        int s;
        try {
            U = getUnsafe();
            Class<?> ek = JucExchanger.class;
            Class<?> nk = Node.class;
            Class<?> ak = Node[].class;
            Class<?> tk = Thread.class;
            BOUND = U.objectFieldOffset
                    (ek.getDeclaredField("bound"));
            SLOT = U.objectFieldOffset
                    (ek.getDeclaredField("slot"));
            MATCH = U.objectFieldOffset
                    (nk.getDeclaredField("match"));
            BLOCKER = U.objectFieldOffset
                    (tk.getDeclaredField("parkBlocker"));
            s = U.arrayIndexScale(ak);
            System.out.println(s);
            // ABASE absorbs padding in front of element 0
            ABASE = U.arrayBaseOffset(ak) + (1 << ASHIFT);

        } catch (Exception e) {
            throw new Error(e);
        }
        if ((s & (s - 1)) != 0 || s > (1 << ASHIFT))
            throw new Error("Unsupported array scale");
    }

    private static Unsafe getUnsafe() {
        try {

            Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
            singleoneInstanceField.setAccessible(true);
            return (Unsafe) singleoneInstanceField.get(null);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
