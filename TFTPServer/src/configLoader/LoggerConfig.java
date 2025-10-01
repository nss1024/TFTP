package configLoader;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LoggerConfig {

    public static void setup(){
        try{
            //Create the log file in the same directory where the jar file is
            Path jarDir = Paths.get(ConfigLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            Path logDir=jarDir.resolve("logs");
            Files.createDirectories(logDir);
            Logger rootLogger = Logger.getLogger("");
            FileHandler fh = new FileHandler(logDir.resolve("tftpServer.log").toString(),1024*1024,5,true);
            fh.setFormatter(new SimpleFormatter());
            rootLogger.addHandler(fh);
            Logger.getLogger("").setLevel(Level.ALL);
        }catch(IOException | URISyntaxException e){
            Logger.getAnonymousLogger().severe("Failed to initialize log file handler: " + e.getMessage());
        }
    }
}
