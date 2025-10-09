package configLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigLoader {

    //String filePath;
    final static String configFileName="appConfig.conf";
    static Logger logger = Logger.getLogger(ConfigLoader.class.getName());
    private static AppConfigs appConfig;
    private final Properties configProps;

    private int serverPort = 0;
    private int portFrom = 0;
    private int portTo = 0;
    private String fileStorePath=null;


    public ConfigLoader() throws URISyntaxException {

        Path dir = getConfigDirPath();
        configProps = getConfigFromFile(dir);

    }

    private  Path getConfigDirPath() throws URISyntaxException {
        Path jarPath = Paths.get(
                ConfigLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI()
        );
        return jarPath.getParent();
    }

    private  Properties getConfigFromFile(Path p) throws URISyntaxException {
        Properties prop = new Properties();
        Path configPath = p.resolve(configFileName);
        File configFile=new File(configPath.toUri());
        if (Files.exists(configPath)){
            try (FileInputStream fis = new FileInputStream(configFile)) {
                prop.load(fis);

            }catch (FileNotFoundException e){
                logger.log(Level.SEVERE,"Config file not found in "+configPath.toString()+ "Starting server with default values!");
                logger.log(Level.INFO,"Failed to parse port numbers, the following defaults will be used: " +
                        "\n Server port:8096 \n Connection port range: 32000 - 33000 \n file store at app directory /FileStore ");
                return null;

            }catch (IOException e){
                logger.log(Level.SEVERE,"IO error while loading config file from "+configPath.toString());
                logger.log(Level.INFO,"Failed to parse port numbers, the following defaults will be used: " +
                        "\n Server port:8096 \n Connection port range: 32000 - 33000 \n file store at app directory /FileStore ");
                return null;
            }
        }
        return prop;
    }

    private AppConfigs loadConfig(Properties p){
        if(p==null){
            try {
                logger.log(Level.INFO,"Loading default configurations: " +
                        "\n Server port:8096 \n Connection port range: 32000 - 33000 \n file store at app directory /FileStore ");
                return AppConfigs.withDefaults();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }else {
            try{
               validateConfigs(p);
                return AppConfigs.withConfigs(serverPort,portFrom,portTo,fileStorePath);
            } catch (IllegalArgumentException e){
                logger.log(Level.SEVERE,"Error parsing configuration file, default configuration will be loaded!");
                try {
                    return AppConfigs.withDefaults();
                } catch (URISyntaxException ex) {
                    logger.log(Level.SEVERE,"Failed to initialize default app configuration");
                    throw new RuntimeException(ex);
                }
            }


        }
    }

    private void validateConfigs(Properties p){
        try {
            serverPort = Integer.parseInt(p.getProperty("app.serverPort"));
            portFrom = Integer.parseInt(p.getProperty("app.serverPortFrom"));
            portTo = Integer.parseInt(p.getProperty("app.serverPortTo"));
            fileStorePath = p.getProperty("app.fileStorePath");
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        if(serverPort==0 || portFrom ==0 || portTo ==0){
            logger.log(Level.SEVERE,"A port number of 0 cannot be used! Please check configuration for:\n" +
                    "server port and port range");
            throw new IllegalArgumentException();
        }

        if(portFrom>=portTo){
            logger.log(Level.SEVERE,"Port range is 0 or negative, please allow more ports for file transfer!" +
                    "Current setting (From - To)"+portFrom+" - "+portTo);
            throw new IllegalArgumentException();
        }
    }

    public AppConfigs getAppConfig() {
        return loadConfig(configProps);
    }
}
