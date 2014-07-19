package at.yawk.accordion.compression;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SnappyCompressorTest {
    @Test
    public void testCompress() {
        ByteBuf unc = Unpooled.buffer();
        unc.writeBytes(new byte[]{ 't', 'e', 's', 't' });

        ByteBuf comp = SnappyCompressor.getInstance().encode(unc);
        ByteBuf unc2 = SnappyCompressor.getInstance().decode(comp);

        unc.resetReaderIndex();
        unc2.resetReaderIndex();

        assertEquals(unc, unc2);
    }
}