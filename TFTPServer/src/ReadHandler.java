import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReadHandler implements Runnable{

    List<Integer> portList;
    byte[] data = null;
    String ip = "";
    int destPort = 0;
    int localPort = 8888;
    DatagramSocket ds = null;
    DatagramPacket dp = null;
    short blockNo=1;
    boolean running = true;
    boolean retry=true;
    final String PATH = "c:/dev/FileStore/";
    Logger logger = Logger.getGlobal();
    int retransmitCounter=0;
    int maxAttempts=3;//TODO make max attempts configurable
    int oldACKCounter=0;
    final int ackTolerance = 5; //TODO make ackTolerance configurable
    List<byte[]> dataBuffer = new ArrayList<byte[]>();

    private ReadHandler(){}

    public ReadHandler(byte [] data, int destPort, String ip, int sessionPort, List<Integer> portList){
        this.data=data;
        this.destPort = destPort;
        this.ip = ip;
        this.portList=portList;
        localPort=sessionPort;
    }

    @Override
    public void run() {
        //get file name
        FileInputStream fis=null;
        ds=TFTPUtils.getDatagramSocket(localPort);
        String fileName = TFTPUtils.getText(data,2,0);
        String mode = TFTPUtils.getText(data,2,1);

        try {
            fis = new FileInputStream(new File(PATH+fileName));
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE,"Failed to open file!");
                TFTPUtils.sendError(ds,1,ip,destPort);
            throw new RuntimeException(e);
        }
        byte [] fileData = new byte[512];
        byte [] inboundBuffer = new byte[1024];
        byte [] inboundData = null;
        int len=0;
        dp=new DatagramPacket(inboundBuffer,inboundBuffer.length);
        while(running){
            try {
                if ((len=fis.read(fileData))==-1) running = false;
                TFTPUtils.sendData(ds,Arrays.copyOf(fileData,len),blockNo,ip,destPort);
                ds.setSoTimeout(1000);//set a 1-second timeout on the read
                retry=true;
                do {
                    try {
                        ds.receive(dp);
                        inboundData = Arrays.copyOf(dp.getData(), dp.getLength());
                        if (TFTPUtils.getShort(inboundData, 0) == (short) 4) {
                            //check block number
                            int responseBlockNo = TFTPUtils.getShort(inboundData, 2);
                            if (responseBlockNo != blockNo) {
                                if (responseBlockNo > blockNo) {//something went wrong here, close the connection
                                    try {
                                        TFTPUtils.closeResources(fis, ds, "Received block number greater than las number sent! " + responseBlockNo);
                                    } catch (IOException e) {
                                        logger.log(Level.WARNING, "Failed to close resource!");
                                    }
                                    throw new RuntimeException();//stop execution
                                }
                                if (responseBlockNo < blockNo) {
                                    logger.log(Level.WARNING, "Received ACK old message, network may be slow! If this continues, transmission will be terminated!");
                                    oldACKCounter++;
                                    TFTPUtils.sendData(ds,Arrays.copyOf(fileData,len),blockNo,ip,destPort);//try and send data again
                                    if (oldACKCounter == ackTolerance) {
                                        try {
                                            TFTPUtils.closeResources(fis, ds, "Received ACK for old message " + oldACKCounter + " times, terminating operation!");
                                        } catch (IOException e) {
                                            logger.log(Level.WARNING, "Failed to close resource!");
                                        }
                                        throw new RuntimeException();//stop execution
                                    }

                                }
                            }else{blockNo++;retry=false;}

                        }
                        if (TFTPUtils.getShort(inboundData, 0) != (short) 4) {
                            //If response is not 04-ACK it means either an error or an illegal TFTP operation, in each case, stop transfer
                            try {
                                TFTPUtils.closeResources(fis, ds, "Received error or illegal op code from the client, transfer cancelled!");
                            } catch (IOException e) {
                                logger.log(Level.WARNING, "Failed to close resource!");
                            }
                            throw new RuntimeException();//stop execution

                        }
                    } catch (SocketTimeoutException e) {
                        //handle re-transmission on timeout
                        retransmitCounter++;
                        if(retransmitCounter==maxAttempts){retry=false;}
                    }
                }while(retry);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        portList.remove(Integer.valueOf(localPort));

    }
}
