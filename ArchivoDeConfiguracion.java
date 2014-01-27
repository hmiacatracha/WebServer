/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ServidorWeb;

import java.io.*;
import java.util.Properties;

/**
 *
 * @author HMIA
 */
public class ArchivoDeConfiguracion {
    private Properties propiedad;
    private int puerto;
    private String directoryIndex;
    private String directory;
    private String allow;
    ArchivoDeConfiguracion() throws IOException,NumberFormatException {
        propiedad = new Properties();
        propiedad.load(new FileInputStream("FileConfiguration.conf"));
        puerto = Integer.parseInt(propiedad.getProperty("PORT"));
        directoryIndex = propiedad.getProperty("DIRECTORY_INDEX");
        directory = propiedad.getProperty("DIRECTORY");
        allow=propiedad.getProperty("ALLOW");
    }
   public String allow(){
       return allow;
   }
    /**
     * @return the puerto
     */
    public int getPuerto() {
        return puerto;
    }
    /**
     * @return the directoryIndex
     */
    public String getDirectoryIndex() {
        return directoryIndex;
    }
    /**
     * @param directoryIndex the directoryIndex to set
     */
    public void setDirectoryIndex(String directoryIndex) {
        this.directoryIndex = directoryIndex;
    }

    /**
     * @return the directory
     */
    public String getDirectory() {
        return directory;
    }

    /**
     * @param directory the directory to set
     */
}
