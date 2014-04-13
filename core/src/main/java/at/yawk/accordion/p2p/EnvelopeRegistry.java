package at.yawk.accordion.p2p;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.apache.mahout.math.set.AbstractLongSet;
import org.apache.mahout.math.set.OpenLongHashSet;

/**
 * Class that manages incoming packet IDs to ensure that no packet is handled twice.
 *
 * @author Jonas Konrad (yawkat)
 */
class EnvelopeRegistry {
    /**
     * The amount of "chunks" packets should be organized in. Each chunk contains a big list of packet IDs that were
     * received during its active period.
     */
    private static final int CHUNK_COUNT = 2;
    /**
     * The length of the gc cycle (in milliseconds). Each cycle, the oldest chunk is cleared and the memory used for the
     * packet IDs received during its active period freed.
     */
    private static final long GARBAGE_COLLECTION_INTERVAL = 1000 * 60 * 10; // 10 minutes

    /**
     * The ID chunks.
     */
    private final AbstractLongSet[] chunks = new OpenLongHashSet[CHUNK_COUNT];

    public EnvelopeRegistry(ScheduledExecutorService executor) {
        clearSets();

        executor.scheduleAtFixedRate(this::cycleSets,
                                     GARBAGE_COLLECTION_INTERVAL,
                                     GARBAGE_COLLECTION_INTERVAL,
                                     TimeUnit.MILLISECONDS);
    }

    /**
     * Clears the entire registry.
     */
    private synchronized void clearSets() {
        for (int i = 0; i < CHUNK_COUNT; i++) { chunks[i] = new OpenLongHashSet(); }
    }

    /**
     * Cycles the sets, effectively disposing the oldest chunk and freeing it for garbage collection.
     */
    private synchronized void cycleSets() {
        System.arraycopy(chunks, 0, chunks, 1, CHUNK_COUNT - 1);
        chunks[0] = new OpenLongHashSet();
    }

    /**
     * Tries to register an envelope and returns true if it should be handled.
     */
    public boolean register(@NonNull Envelope envelope) {
        return register(envelope.getId());
    }

    /**
     * Tries to register a packet ID and returns true if it was not registered before.
     */
    public synchronized boolean register(long id) {
        // check in old chunks
        for (int i = 1; i < CHUNK_COUNT; i++) {
            if (chunks[i].contains(id)) { return false; }
        }
        // check latest chunk
        return chunks[0].add(id);
    }
}
