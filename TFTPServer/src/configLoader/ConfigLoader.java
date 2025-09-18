package configLoader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {

    String filePath;
    final String configFileName="appConfig.conf";

    ConfigLoader(){
        if(System.getProperty("os.name").contains("Windows")){
            filePath="c:/dev/TFTP/";
        }else{
            filePath="/dev/TFTP/";
        }
    }

    private void getConfigFromFile(String fileName, String path){
        Properties prop = new Properties();

        try (FileInputStream fis = new FileInputStream(path+fileName)) {
            prop.load(fis);
        } catch (FileNotFoundException ex) {
            System.out.println("Configuration file"+fileName+"not found at location :"+path+" "+ex);
        } catch (IOException ex) {
            System.out.println("An error occurred while reading the configuration file! "+ex);
        }

    }



}
