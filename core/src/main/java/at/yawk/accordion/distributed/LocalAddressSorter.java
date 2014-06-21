package at.yawk.accordion.distributed;

import java.net.InetAddress;
import java.util.Comparator;

/**
 * InetAddress comparator that prefers addresses close to a given address.
 *
 * @author yawkat
 */
public class LocalAddressSorter implements Comparator<InetAddress> {
    /**
     * Our address.
     */
    private final byte[] self;

    public LocalAddressSorter(InetAddress address) {
        self = address.getAddress();
    }

    @Override
    public int compare(InetAddress o1, InetAddress o2) {
        byte[] b1 = o1.getAddress();
        byte[] b2 = o2.getAddress();
        // different IP types
        if (b1.length != b2.length || b1.length != self.length) {
            return 0;
        }
        // greater is better
        return score(b2) - score(b1);
    }

    /**
     * Calculate the "score" / distance of the given address to our address. The greater, the closer.
     */
    private int score(byte[] of) {
        int i = 0;
        while (i < of.length && of[i] == self[i]) {
            i++;
        }
        return i;
    }
}
