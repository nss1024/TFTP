import configLoader.ConfigLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WriteDataHandler implements Runnable{
    List<Integer> portList;
    byte[] data = null;
    String ip = "";
    int destPort = 0;
    int localPort = 8888;
    DatagramSocket ds = null;
    DatagramPacket dp = null;
    short blockNo=0;
    boolean running = true;
    final String PATH = "c:/dev/TFTP/FileStore/";
    Logger logger = Logger.getLogger(WriteDataHandler.class.getName());
    int duplicatePacketCounter;
    List<byte[]> dataBuffer = new ArrayList<byte[]>();

    private WriteDataHandler(){}

   public WriteDataHandler(byte [] data, int destPort, String ip, int sessionPort, List<Integer> portList){
        this.data=data;
        this.destPort = destPort;
        this.ip = ip;
        this.portList=portList;
        localPort=sessionPort;
    }

    @Override
    public void run() {
        createDatagramSocket();
        String fileName="";
        String mode="";
        FileOutputStream fos=null;
        try {//handle file setup
            //get filename from datagram packet
            fileName = getText(data, 2, 0);
            //try to create file
            fos = new FileOutputStream(new File(PATH + "/" + fileName));
        } catch (FileNotFoundException e) {
            logger.log(Level.WARNING, "Error creating file! ");
            try {
               sendError(ds,2);
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Failed to send error message to client!" + ip);
            }
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Error parsing byte array! ");
            throw new RuntimeException();
        }

        //check mode; Supported mode = octet, NetAscii
        try {
            mode = getText(data, 2, 1);//get mode from datagram packet
            if (!mode.equalsIgnoreCase("mail")) {
                try {
                    sendError(ds, 0);
                    logger.log(Level.WARNING, "Only octet or netascii mode supported, received request for " + mode + " mode");
                    throw new RuntimeException();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE,"Unable to verify mode!");
            throw new RuntimeException(e);
        }

            //else reply with ACK and start reading loop
            try {
                sendACK(ds,(short)0);
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Failed to send error message to client!" + ip);
            }


            while(running){//if all is well, start listening for inbound packets.
                byte [] buffer = new byte [516]; //Packets should be 516 bytes 2byte OP Code + 2bye Block number + 512 bytes data
                try {
                    ds.receive(dp);
                    buffer = Arrays.copyOf(dp.getData(),dp.getData().length);//get data from datagram packet, copying just in case, no other packet should arrive without us sending an ack
                    if(buffer.length<516){running=false;}//last data packed received
                    if(getOpCode(buffer)!=3){//only accept data packets, if anything else, log it and exit loop
                        logger.log(Level.WARNING,"Non data op code received"+getOpCode(buffer));
                        sendError(ds,5);
                        break;
                    }
                    //check for duplicate data packets
                    //TODO: make duplicate counter configurable
                    if(getBlockNo(buffer)==blockNo){
                        logger.log(Level.WARNING,"Duplicate packet detected");
                        if(duplicatePacketCounter==3){
                            sendError(ds,4);
                            break;
                        }
                        duplicatePacketCounter++;
                    }
                    blockNo=getBlockNo(buffer);//update block number
                    dataBuffer.add(Arrays.copyOfRange(buffer,4,buffer.length));//save data to Arraylist
                    sendACK(ds,getBlockNo(buffer));
                } catch (IOException ex) {
                    logger.log(Level.WARNING,"Error processing data packet!");
                }
            }
        for (byte[] bytes : dataBuffer) {
            try {
                fos.write(bytes);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error writing to file");
            }
        }
        try {
            fos.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to close file");
        }

        portList.remove(Integer.valueOf(localPort));


    }

        private short getBlockNo ( byte[] b){
            return getShort(b, 2);
        }

        private short getOpCode ( byte[] b){
            return getShort(b, 0);
        }

        private String getText ( byte[] b, int offset, int mode) throws IllegalArgumentException {
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

        private short getShort ( byte[] b, int startIndex){
            short result = (short) 0;
            try {
                result = (short) (((b[startIndex] & 0XFF) << 8) | (b[startIndex + 1] & 0XFF));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error getting short from byte array! ");
            }
            return result;
        }

        private String getError ( int code){
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

        private void sendACK(DatagramSocket ds, short blockNo) throws IOException{
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.putShort((short) 4);//op code for ACK
            bb.putShort((short) blockNo);//Block number of 0 to indicate transfer can be started

            try {
                ds.send(new DatagramPacket(bb.array(), bb.array().length, InetAddress.getByName(ip), destPort));
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Failed to send error message to client!" + ip);
            }
        }

        private void sendError(DatagramSocket ds, int errorCode) throws IOException{

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

        private void createDatagramSocket(){
            try {
                ds = new DatagramSocket(localPort);//create local datagram socket
            } catch (SocketException e) {
                logger.log(Level.WARNING,"Error initializing datagram socket!");
                throw new RuntimeException();
            }
        }


}
