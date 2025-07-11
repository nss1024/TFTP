import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    public static void main(String[] args) {
        Logger logger = Logger.getGlobal();
        int serverPort=8069;
        int portRangeFrom=32000;
        int portRangeTo = 33000;

        //get ports
        try{
            serverPort = Integer.parseInt(args[0]);
            portRangeFrom = Integer.parseInt(args[1]);
            portRangeTo = Integer.parseInt(args[2]);

        }catch(Exception e){
            logger.log(Level.INFO,"Failed to parse port numbers, the following defaults will be used: " +
                    "\n Server port:"+serverPort+"\n Connection port range: "+portRangeFrom+" - "+portRangeTo);
        }

        //start server
        logger.log(Level.INFO,"Creating server!");
        Server server = new Server(serverPort,portRangeFrom,portRangeTo);
        server.startServer();
    }
}