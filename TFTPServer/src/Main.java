import configLoader.ConfigLoader;
import configLoader.LoggerConfig;

import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    public static void main(String[] args) {
        Logger logger = Logger.getLogger(Main.class.getName());
        int serverPort=8069;
        int portRangeFrom=32000;
        int portRangeTo = 33000;

        try {
            ConfigLoader.getConfigFromFile(ConfigLoader.getConfigDirPath());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        //Configure logging
        LoggerConfig.setup();

        //start server
        logger.log(Level.INFO,"Creating server!");
        Server server = new Server(serverPort,portRangeFrom,portRangeTo);
        server.startServer();
    }
}