package at.yawk.accordion.distributed;

import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.util.Optional;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class used to ensure that packets are not handled multiple times. Each packet has a random long packet ID, which is
 * added to this handler. When a packet is received, the network manager can check if this packet ID has already been
 * received and take appropriate action. This handler will delete its entries after they have been in it for a specific
 * time to avoid memory leaks.
 *
 * @author yawkat
 */
class PacketDistinctionHandler {
    /*
     * Implementation details:
     *
     * We have an array of "shifts" (2) which store packet IDs for a specific time window. A thread clears the last
     * segment every 60 seconds, removing the oldest packet IDs (1 - 2 minutes old). The array is shifted by one and
     * a new shift is prepended.
     */

    /**
     * Segment / shift count we should use. 2 makes the most sense here.
     */
    private static final int SEGMENT_COUNT = 2;
    /**
     * Interval when we should clear the oldest shift and start a new one. Defaults to one minute.
     */
    private static final long DEFAULT_CLEAR_INTERVAL = TimeUnit.MINUTES.toMillis(1);

    /**
     * Atomic integer holding the thread ID for the clearing threads created with the default thread factory.
     */
    private static final AtomicInteger THREAD_ID = new AtomicInteger();

    /**
     * Shifts in this distinction handler.
     */
    private final Shift[] shifts = new Shift[SEGMENT_COUNT];

    /**
     * Thread used to clear shifts.
     */
    private Optional<Thread> clearThread = Optional.empty();

    private PacketDistinctionHandler() {
        // create shifts
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            shifts[i] = new Shift();
        }
    }

    /**
     * Create a new PacketDistinctionHandler and start its clearing thread with the default interval.
     */
    public static PacketDistinctionHandler createAndStart() {
        return createAndStart(DEFAULT_CLEAR_INTERVAL);
    }

    /**
     * Create a new PacketDistinctionHandler and start its clearing thread with the given interval.
     */
    public static PacketDistinctionHandler createAndStart(long clearIntervalMillis) {
        PacketDistinctionHandler handler = createNotStarted();
        handler.startAutoClear(task -> new Thread(task, "Packet distinction thread #" + THREAD_ID.incrementAndGet()),
                               clearIntervalMillis);
        return handler;
    }

    /**
     * Create a new PacketDistinctionHandler without starting its clearing thread.
     */
    public static PacketDistinctionHandler createNotStarted() {
        return new PacketDistinctionHandler();
    }

    /**
     * Start clearing thread.
     *
     * @throws java.lang.IllegalStateException if a clearing thread is already running.
     */
    public synchronized void startAutoClear(ThreadFactory threadFactory, long intervalMillis) {
        /*
         * This doesn't use shiftLock because it's only synchronized to avoid simultaneous calls of the same method.
         */

        // check for running thread
        clearThread.ifPresent(thread -> {
            if (thread.isAlive()) {
                throw new IllegalStateException("Clear thread already running: " + thread);
            }
        });

        Thread thread = threadFactory.newThread(() -> {
            try {
                // until interrupt
                while (!Thread.interrupted()) {
                    // wait for the interval
                    Thread.sleep(intervalMillis);
                    // clear oldest shift
                    clearLastShift();
                }
            } catch (InterruptedException ignored) {
                // interrupted in Thread.sleep, stop
            }
        });
        thread.setDaemon(true);
        thread.start();
        clearThread = Optional.of(thread);
    }

    /**
     * Clear the last shift and prepend a new one to the array.
     */
    private synchronized void clearLastShift() {
        Shift last = shifts[SEGMENT_COUNT - 1];
        // shift array by one
        System.arraycopy(shifts, 0, shifts, 1, SEGMENT_COUNT - 1);
        // move last shift to first place and clear so it can be reused
        shifts[0] = last;
        last.clear();
    }

    /**
     * Attempt to register a packet ID.
     *
     * @return Whether the registration was successful (the packet ID was not registered before).
     */
    public synchronized boolean register(long newId) {
        for (int i = 1; i < SEGMENT_COUNT; i++) {
            if (shifts[i].contains(newId)) {
                return false;
            }
        }
        return shifts[0].register(newId);
    }

    /**
     * Represents one shift in this handler.
     *
     * Not thread-safe.
     */
    private class Shift {
        /**
         * The set of IDs registered in this shift.
         */
        private final TLongSet usedIds = new TLongHashSet();

        /**
         * Register a new ID.
         *
         * @return <code>true</code> if this ID was previously unregistered.
         */
        public boolean register(long newId) {
            return usedIds.add(newId);
        }

        /**
         * Check if an ID was previously registered in this shift.
         */
        public boolean contains(long id) {
            return usedIds.contains(id);
        }

        /**
         * Clear all IDs in this shift.
         */
        public void clear() {
            usedIds.clear();
        }
    }
}
