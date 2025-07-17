import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
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
    short blockNo=1;
    final String PATH = "c:/dev/FileStore/";
    Logger logger = Logger.getGlobal();
    private final int MAX_ATTEMPTS=3;
    private final int TIMEOUT_DURATION=1000;
    private static final int DATA_BLOCK_SIZE = 512;



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
        ds=TFTPUtils.getDatagramSocket(localPort,TIMEOUT_DURATION);
        String fileName = TFTPUtils.getText(data,2,0);
        String mode = TFTPUtils.getText(data,2,1);
        if(!TFTPUtils.isFileNameValid(fileName)) {
            logger.log(Level.SEVERE,"Received invalid file name!");
            TFTPUtils.sendError(ds,0,"Invalid filename received:"+fileName,ip,destPort);
            TFTPUtils.closeResources(ds);
            portList.remove(Integer.valueOf(localPort));
            return;
        }
        sendFile(PATH, fileName, ds, blockNo, ip, destPort);
        TFTPUtils.closeResources(ds);
        portList.remove(Integer.valueOf(localPort));

    }

    private void sendFile(String path,String fileName, DatagramSocket ds,short blockNo,String ip, int destPort){
        byte[] fileData = new byte[DATA_BLOCK_SIZE];
        int bytes;
        int counter=0;
        File f = new File(path+fileName);
        try(FileInputStream fis=new FileInputStream(f)){
        while((bytes=fis.read(fileData)) > -1){
                    if(sendData(ds,Arrays.copyOf(fileData,bytes),blockNo,ip,destPort)){
                        blockNo=(short)((blockNo+1)&0XFFFF);
                        counter++;
                    }else{
                        logger.log(Level.SEVERE,"Failed to send data block to "+ip+":"+destPort);
                        TFTPUtils.sendError(ds,0,"Failed to transmit file!"+fileName,ip,destPort);
                        return;
                    }
        }
        logger.log(Level.INFO,"File transmitted successfully."+counter+"blocks were sent to "+ip+":"+destPort);


        }catch(FileNotFoundException fe){
            logger.log(Level.SEVERE,"Error opening file "+fileName);
            TFTPUtils.sendError(ds,0, "File not found: " + fileName, ip, destPort);

        }catch(IOException e){
            logger.log(Level.SEVERE,"Error sending file to client \nFile name:"+
            fileName+"\n destination: "+ip+":"+destPort);

        }
    }

    private boolean sendData(DatagramSocket ds, byte[] dataToSend,short blockNo,String ip,int destPort){

        int retries = 0;
        byte[] reply;
        while (true){
            TFTPUtils.sendData(ds,dataToSend,blockNo,ip,destPort);
            reply=waitForReply(ds);
            if(reply!=null&&evaluateReply(reply,blockNo)){return true;}
            if(retries==MAX_ATTEMPTS){
                logger.log(Level.WARNING, "Max retries reached for block " + blockNo);
                return false;
            }
            else{retries++;}
        }
    }

    private byte[] waitForReply(DatagramSocket ds){
        byte [] inboundBuffer = new byte[1024];
        DatagramPacket dp=new DatagramPacket(inboundBuffer,inboundBuffer.length);
        try {
            ds.receive(dp);
            return Arrays.copyOf(dp.getData(),dp.getLength());
        }catch(SocketTimeoutException t){
            logger.log(Level.WARNING,"Client response timed out!");
            return null;
        }catch (IOException e) {
            logger.log(Level.SEVERE,"Failed to receive inbound data packet!");
            return null;
        }
    }

    private boolean evaluateReply(byte[] b,short blockNo){
        if(TFTPUtils.isAck(b)){
            return TFTPUtils.isValidAck(b,blockNo);
        }else if(TFTPUtils.isError(b)){
            logger.log(Level.SEVERE,"Client Error received"+TFTPUtils.getShort(b,2)+" - "
                    +TFTPUtils.getError(TFTPUtils.getShort(b,2)));
            return false;
        } else{
            logger.log(Level.SEVERE,"Unsupported OP code returned!"+TFTPUtils.getOpCode(b));
            return false;
        }
    }

}

