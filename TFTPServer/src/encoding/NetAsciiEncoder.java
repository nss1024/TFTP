package encoding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class NetAsciiEncoder {
    InputStream is = null;

    private NetAsciiEncoder(){}

    public NetAsciiEncoder(InputStream is){
        this.is=is;
    }

    public byte[] checkBytesNetASCII() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(512);

        boolean crDetected = false;
        int byteIn;

        while (buffer.size() < 512 && (byteIn = is.read()) != -1) {
            if (byteIn=='\r') {
                crDetected = true;
                continue;
            }

            if (crDetected) {
                if (byteIn=='\n') {
                    buffer.write('\r');
                    buffer.write('\n');
                } else {
                    buffer.write('\r');
                    buffer.write('\0');
                    buffer.write(byteIn);
                }
                crDetected = false;
            } else {
                buffer.write(byteIn);
            }
        }

        // Handle case where stream ends after a \r
        if (crDetected) {
            buffer.write('\r');
            buffer.write('\0');
        }

        return buffer.toByteArray();
    }//encodes outgoing bytes t netASCII (change /r to either /r/n or /r/0)



}
