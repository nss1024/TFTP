import configLoader.AppConfigs;
import configLoader.ConfigLoader;
import configLoader.LoggerConfig;
import watchdog.WatchDogMain;

import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    public static void main(String[] args) {
        Logger logger = Logger.getLogger(Main.class.getName());
        AppConfigs appConfigs;
        try {
            ConfigLoader configloader = new ConfigLoader();
            configloader.loadAppConfig();//this will either load config from a config file or load default config;
            appConfigs = AppConfigs.getAppConfigs();

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        WatchDogMain watchDogMain = new WatchDogMain();
        watchDogMain.start();
        logger.log(Level.INFO,"Watchdog started!");

        //Configure logging
        LoggerConfig.setup();

        //start server
        logger.log(Level.INFO,"Creating server!");
        Server server = new Server(appConfigs.getServerPort(), appConfigs.getPortRangeFrom(), appConfigs.getPortRangeTo());
        server.startServer();
    }
}