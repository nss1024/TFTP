package configLoader;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;



public class AppConfigs {

    private int serverPort = 0;
    private int portRangeFrom = 0;
    private int portRangeTo = 0;
    private final Path fileStorePath;
    private static AppConfigs appConfigs;


    public AppConfigs(int serverPort,int portRangeFrom, int portRangeTo, String fileStorePath){
        this.serverPort=serverPort;
        this.portRangeFrom=portRangeFrom;
        this.portRangeTo=portRangeTo;
        this.fileStorePath= Paths.get(fileStorePath).resolve("FileStore");
    }

    public static AppConfigs withDefaults() throws URISyntaxException {
        if(appConfigs==null) {
            appConfigs = new AppConfigs(8069, 32000, 33000, defaultFileStore().toString());
        }
        return appConfigs;
    }

    public static AppConfigs withConfigs(int serverPort,int portRangeFrom,int portRangeTo, String path){
        if(appConfigs==null) {
            appConfigs = new AppConfigs(serverPort, portRangeFrom, portRangeTo, path);
        }
        return appConfigs;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getPortRangeFrom() {
        return portRangeFrom;
    }

    public int getPortRangeTo() {
        return portRangeTo;
    }

    public Path getFileStorepath() {
        return fileStorePath;
    }


    private static Path defaultFileStore() throws URISyntaxException {
        return Paths.get(ConfigLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }
}
