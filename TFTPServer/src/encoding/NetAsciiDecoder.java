package encoding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class NetAsciiDecoder {
    boolean crFound=false;
    ByteArrayOutputStream buffer = new ByteArrayOutputStream(512);
    byte[] systemEol = System.lineSeparator().getBytes();

    public byte[] decodeNetAscii(byte[] data) throws IOException {//needs the 512 bytes of data vs the 516 received
        for (byte b : data) {
            if (crFound) {
                if (b == '\n') {
                    buffer.write(System.lineSeparator().getBytes());
                } else if (b == '\0') {
                    buffer.write('\r');
                } else {
                    buffer.write('\r');
                    buffer.write(b);
                }
                crFound = false;
            } else if (b == '\r') {
                crFound = true;
            } else {
                buffer.write(b);
            }
        }
        return buffer.toByteArray();
    }

    public byte[] flush() throws IOException {
        if (crFound) {
            buffer.write('\r');  // trailing CR not followed by LF or NULL
            crFound = false;
        }
        return buffer.toByteArray();
    }

    public void reset() {
        buffer.reset();
        crFound = false;
    }
}
