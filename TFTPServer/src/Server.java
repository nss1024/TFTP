/*Assumptions:
 * buffer array - set to 2048 bytes to accommodate larger file names
 */
import configLoader.AppConfigs;
import configLoader.ConfigLoader;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    AppConfigs appConfigs = AppConfigs.getAppConfigs();
    private Logger logger = Logger.getLogger(Server.class.getName());
    private int port = 0;
    private final int threads = Runtime.getRuntime().availableProcessors()>1?Runtime.getRuntime().availableProcessors()/2:1;
    private DatagramSocket ds = null;
    private ExecutorService executor = null;
    private ThreadPoolExecutor threadPoolExecutor = null;
    BlockingQueue<Runnable> workQueue=new LinkedBlockingQueue();
    private boolean running = true;
    private int portRangeFrom= appConfigs.getPortRangeFrom();
    private int portRangeTo = appConfigs.getPortRangeTo();
    private final List<Integer> portList = new CopyOnWriteArrayList<>();
    int sessionPort=0;

    Server(int port, int portRangeFrom, int portRangeTo){
        this.port=port;
        this.portRangeFrom=portRangeFrom;
        this.portRangeTo=portRangeTo;
        logger.log(Level.INFO,"Server instantiated!");
    }

    private void createServer(){
        try {
            ds=new DatagramSocket(port);
        } catch (SocketException e) {
            logger.log(Level.WARNING,"Failed to start Server");
            throw new RuntimeException(e);
        }
        logger.log(Level.INFO,"Server started");
    }

    private void startExecutorPool(int threads){
        threadPoolExecutor=new ThreadPoolExecutor(2,threads,60L, TimeUnit.SECONDS,workQueue);
        logger.log(Level.INFO,"Thread pool executor started! Using "+threads+" threads!");
    }

    private void stopExecutorService(){executor.shutdown();}

    public void stopServer(){running=false; ds.close();stopExecutorService();}

    private int getthreadPortNumber(){

       return TFTPUtils.getLocalPort(portRangeFrom,portRangeTo,portList);
    }

    public void startServer(){
        createServer();
        startExecutorPool(threads);
        logger.log(Level.INFO,"Server listening on port "+port);
        byte [] buffer = new byte [2048];
        int dpPort = 0;
        String dpIpAddress="";
        DatagramPacket dp = new DatagramPacket(buffer,buffer.length);
        while(running){
            try {
                ds.receive(dp);
                byte[] data = Arrays.copyOf(dp.getData(),dp.getData().length);
                dpPort=dp.getPort();
                dpIpAddress=dp.getAddress().getHostAddress();

                switch(getOpCode(data)) {
                    case 1://read request RRQ

                        sessionPort = getthreadPortNumber();
                        threadPoolExecutor.submit(new ReadHandler(data,dpPort,dpIpAddress,sessionPort,portList));
                        break;
                    case 2://write request WRQ

                        sessionPort = getthreadPortNumber();
                        threadPoolExecutor.submit(new WriteDataHandler(data,dpPort,dpIpAddress,sessionPort,portList));
                        break;
                    default://send error code, received a malformed packet with unknown error code
                        break;
                }

            } catch (IOException e) {

                logger.log(Level.WARNING,"Failed to retrieve data packet! An IO exception has occurred!");
            }catch(IndexOutOfBoundsException e){
                logger.log(Level.WARNING,"Failed to retrieve data packet! Incoming data is greater than 2048 bytes!");
            }

        }
    }

    private short getOpCode(byte[] b){
        ByteBuffer bb = ByteBuffer.wrap(b);
        return bb.getShort();
    }


}
