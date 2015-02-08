/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.compression;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class SnappyCompressorTest {
    @Test
    public void testCompress() {
        Random random = new Random(0);
        for (int i = 0; i < 10000; i++) {
            byte[] data = new byte[random.nextInt(10000)];
            for (int j = 0; j < data.length; j++) data[j] = (byte) random.nextInt(256);
            testCompressDecompress(data);
        }
    }

    private void testCompressDecompress(byte[] payload) {
        ByteBuf unc = Unpooled.buffer();
        unc.writeBytes(payload);

        ByteBuf comp = SnappyCompressor.getInstance().encode(unc);
        ByteBuf unc2 = SnappyCompressor.getInstance().decode(comp);

        unc.resetReaderIndex();
        unc2.resetReaderIndex();

        assertEquals(unc, unc2);
    }
}