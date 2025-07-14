import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TFTPUtils {

    private static final Logger logger = Logger.getGlobal();

    private TFTPUtils(){}

    public static short getBlockNo ( byte[] b){
        return getShort(b, 2);
    }

    public static short getOpCode ( byte[] b){
        return getShort(b, 0);
    }

    public static String getText ( byte[] b, int offset, int mode) throws IllegalArgumentException {
        //Mode 1 = get the second block of text after encountering the first 0 terminator, this is useful when getting the mode
        if (offset >= b.length) {
            throw new IllegalArgumentException("Offset out of bounds.");
        }

        int start = offset;
        int end;

        // Step 1: Find the first null terminator
        end = start;
        while (end < b.length && b[end] != 0) {
            end++;
        }

        if (end == b.length) {
            throw new IllegalArgumentException("Missing null terminator after first string.");
        }

        if (mode == 0) {
            // Extract filename
            return new String(b, start, end - start, StandardCharsets.UTF_8);
        }

        // Step 2: Extract the second string (mode)
        start = end + 1; // move past first null terminator
        if (start >= b.length) {
            throw new IllegalArgumentException("No second string found.");
        }

        end = start;
        while (end < b.length && b[end] != 0) {
            end++;
        }

        if (end == b.length) {
            throw new IllegalArgumentException("Missing null terminator after second string.");
        }

        return new String(b, start, end - start, StandardCharsets.UTF_8);
    }

    public static short getShort ( byte[] b, int startIndex){
        short result = (short) 0;
        try {
            result = (short) (((b[startIndex] & 0XFF) << 8) | (b[startIndex + 1] & 0XFF));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting short from byte array! ");
        }
        return result;
    }

    public static String getError ( int code){
        switch (code) {
            case 0:
                return "Not defined";

            case 1:
                return "File not found";

            case 2:
                return "Access violation";

            case 3:
                return "Disk full or allocation exceeded";

            case 4:
                return "Illegal TFTP operation";

            case 5:
                return "Unknown transfer ID";

            case 6:
                return "File already exists";

            case 7:
                return "No such user";

            default:
                return "Unknown error";

        }
    }

    public static void sendACK(DatagramSocket ds, short blockNo, String ip, int destPort) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putShort((short) 4);//op code for ACK
        bb.putShort((short) blockNo);//Block number of 0 to indicate transfer can be started

        try {
            ds.send(new DatagramPacket(bb.array(), bb.array().length, InetAddress.getByName(ip), destPort));
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to send error message to client!" + ip);
        }
    }

    public static void sendError(DatagramSocket ds, int errorCode, String ip, int destPort) throws IOException{

        ByteBuffer bb = ByteBuffer.allocate(5 + getError(errorCode).length());
        bb.putShort((short) 5);//op code indicating error
        bb.putShort((short) errorCode);//Specific error code
        bb.put(getError(errorCode).getBytes(StandardCharsets.UTF_8));//error message
        bb.put((byte) 0);//"0" terminator

        try {
            ds.send(new DatagramPacket(bb.array(), bb.array().length, InetAddress.getByName(ip), destPort));
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to send error message to client!" + ip);
        }

    }

    public static DatagramSocket getDatagramSocket(int localPort){
        try {
            return new DatagramSocket(localPort);//create local datagram socket
        } catch (SocketException e) {
            logger.log(Level.WARNING,"Error initializing datagram socket!");
            throw new RuntimeException();
        }
    }

    public static int sendData(DatagramSocket ds,byte[] b,short blockNo,String ip, int destPort){
        ByteBuffer bb = ByteBuffer.allocate(516);
        bb.putShort((short)3);
        bb.putShort(blockNo);
        bb.put(b);

        try {
            ds.send(new DatagramPacket(bb.array(),bb.array().length,InetAddress.getByName(ip), destPort));
            return 0;
        } catch (IOException e) {
            logger.log(Level.WARNING,"Failed to send data packet!");
            return -1;
        }
    }

}
