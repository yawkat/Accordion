package at.yawk.accordion.minecraft;

import at.yawk.accordion.p2p.Recipient;
import at.yawk.accordion.p2p.RecipientRange;
import lombok.RequiredArgsConstructor;

/**
 * A category of servers, for example game servers, hub servers or similar.
 *
 * @author Yawkat
 */
public interface ServerCategory extends RecipientRange {
    /**
     * Unique ID of this category used for serialization and deserialization.
     */
    byte getId();

    /**
     * Default categories that are probably sufficient for most networks.
     */
    @RequiredArgsConstructor
    public static enum Default implements ServerCategory {
        /**
         * A category that targets ALL servers: essentially a broadcast category.
         */
        ALL((byte) 0) {
            @Override
            public boolean containsAll(RecipientRange range) {
                return true;
            }
        },
        /**
         * BungeeCord servers.
         */
        BUNGEE((byte) 1),
        /**
         * Bukkit / Spigot servers.
         */
        BUKKIT((byte) 2);

        private final byte id;

        @Override
        public byte getId() {
            return id;
        }

        @Override
        public boolean contains(Recipient recipient) {
            return containsAll(((Server) recipient).getCategory());
        }

        @Override
        public boolean containsAll(RecipientRange range) {
            return this == range;
        }
    }
}
