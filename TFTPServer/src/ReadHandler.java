import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
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
    short blockNo=0;
    boolean running = true;
    final String PATH = "c:/dev/FileStore/";
    Logger logger = Logger.getGlobal();
    int duplicatePacketCounter;
    List<byte[]> dataBuffer = new ArrayList<byte[]>();

    private ReadHandler(){}

    ReadHandler(byte [] data, int destPort, String ip, int sessionPort, List<Integer> portList){
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
            try {
                TFTPUtils.sendError(ds,1,ip,destPort);
            } catch (IOException ex) {
                logger.log(Level.SEVERE,"Failed to sed Error code to client !");
                throw new RuntimeException(ex);
            }
            throw new RuntimeException(e);
        }
        byte [] filedata = new byte[512];

        while(true){
            try {
                if (!(fis.read(filedata)==-1)) break;
                TFTPUtils.sendData(ds,filedata,blockNo,ip,destPort);
                blockNo++;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

    }
}
