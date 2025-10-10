import configLoader.AppConfigs;
import configLoader.ConfigLoader;
import encoding.NetAsciiDecoder;
import watchdog.DataStore;
import watchdog.WatchDogMonitoredSession;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WriteDataHandler implements Runnable, WatchDogMonitoredSession {
    AppConfigs appConfigs = AppConfigs.getAppConfigs();
    List<Integer> portList;
    byte[] data = null;
    String ip = "";
    int destPort = 0;
    int localPort = TFTPUtils.getLocalPort(appConfigs.getPortRangeFrom(),appConfigs.getPortRangeTo(),portList);
    private final int SOCKET_CREATE_TIMEOUT=60;
    DatagramSocket ds = null;
    DatagramPacket dp = null;
    short blockNo=0;
    private volatile boolean running = true;
    final String PATH = appConfigs.getFileStorepath().toString();
    Logger logger = Logger.getLogger(WriteDataHandler.class.getName());
    int duplicatePacketCounter;
    List<byte[]> dataBuffer = new ArrayList<byte[]>();
    NetAsciiDecoder netAsciiDecoder = new NetAsciiDecoder();
    private Thread sessionThread;


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
        sessionThread=Thread.currentThread();
        DataStore.addSessionToDataStore(this);
        ds=TFTPUtils.getDatagramSocket(localPort,SOCKET_CREATE_TIMEOUT);
        String fileName="";
        String mode="";
        FileOutputStream fos=null;
        try {//handle file setup
            //get filename from datagram packet
            fileName = TFTPUtils.getText(data, 2, 0);
            //try to create file
            fos = new FileOutputStream(new File(PATH + "/" + fileName));
        } catch (FileNotFoundException e) {
            logger.log(Level.WARNING, "Error creating file! ");
               TFTPUtils.sendError(ds,2,TFTPUtils.getError(2),ip,destPort);
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Error parsing byte array! ");
            throw new RuntimeException();
        }

        //check mode; Supported mode = octet, NetAscii
        try {
            mode = TFTPUtils.getText(data, 2, 1).toLowerCase();//get mode from datagram packet
            if (!mode.equals("octet") && !mode.equals("netascii")) {

                    TFTPUtils.sendError(ds,0,TFTPUtils.getError(0),ip,destPort);
                    logger.log(Level.WARNING, "Only octet or netascii mode supported, received request for " + mode + " mode");
                    throw new RuntimeException();

            }
        } catch (Exception e) {
            logger.log(Level.SEVERE,"Unable to verify mode!");
            throw new RuntimeException(e);
        }

            //else reply with ACK and start reading loop
                TFTPUtils.sendACK(ds,(short)0,ip,destPort);



            while(running){//if all is well, start listening for inbound packets.
                byte [] buffer = new byte [516]; //Packets should be 516 bytes 2byte OP Code + 2bye Block number + 512 bytes data
                try {
                    ds.receive(dp);
                    buffer = Arrays.copyOf(dp.getData(),dp.getData().length);//get data from datagram packet, copying just in case, no other packet should arrive without us sending an ack
                    if(buffer.length<516){running=false;}//last data packed received

                    if(TFTPUtils.getOpCode(buffer)!=3){//only accept data packets, if anything else, log it and exit loop
                        logger.log(Level.WARNING,"Non data op code received"+TFTPUtils.getOpCode(buffer));
                        TFTPUtils.sendError(ds,5,TFTPUtils.getError(5),ip,destPort);
                        break;
                    }

                    //check for duplicate data packets
                    //TODO: make duplicate counter configurable
                    if(TFTPUtils.getBlockNo(buffer)==blockNo){
                        logger.log(Level.WARNING,"Duplicate packet detected");
                        if(duplicatePacketCounter==3){
                            TFTPUtils.sendError(ds,4,TFTPUtils.getError(4),ip,destPort);
                            break;
                        }
                        duplicatePacketCounter++;
                    }
                    blockNo=TFTPUtils.getBlockNo(buffer);//update block number
                    dataBuffer.add(Arrays.copyOfRange(buffer,4,buffer.length));//save data to Arraylist
                    TFTPUtils.sendACK(ds,TFTPUtils.getBlockNo(buffer),ip,blockNo);
                } catch (IOException ex) {
                    logger.log(Level.WARNING,"Error processing data packet!");
                }
            }
        // if octet received, write directly to file, else, need to decode NETASCII
            for (byte[] bytes : dataBuffer) {
                if(mode.equals("octet")) {
                    try {
                        // if octet received, write directly to file, else, need to decode NETASCII
                        fos.write(bytes);
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Error writing to file");
                    }
                }
                if(mode.equals("netascii")) {
                    try {
                        byte[] decodedBytes = netAsciiDecoder.decodeNetAscii(bytes);
                        fos.write(decodedBytes);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        }
        try {
            fos.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to close file");
        }

        portList.remove(Integer.valueOf(localPort));


    }
        public boolean isAlive(){
        return running;
        }

        public void stopSession(){
            sessionThread.interrupt();
        }


}
