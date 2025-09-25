package configLoader;

import java.nio.file.Path;
import java.nio.file.Paths;

public class AppConfigs {

    private int serverPort = 0;
    private int portRangeFrom = 0;
    private int portRangeTo = 0;
    private Path fileStorePath;

    public AppConfigs(){}

    public AppConfigs(int serverPort,int portRangeFrom, int portRangeTo, String fileStorePath){
        this.serverPort=serverPort;
        this.portRangeFrom=portRangeFrom;
        this.portRangeTo=portRangeTo;
        this.fileStorePath= Paths.get(fileStorePath);
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public void setPortRangeFrom(int portRangeFrom) {
        this.portRangeFrom = portRangeFrom;
    }

    public void setPortRangeTo(int portRangeTo) {
        this.portRangeTo = portRangeTo;
    }

    public void setFileStorepath(String fileStorePath) {
        this.fileStorePath= Paths.get(fileStorePath);
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
}
