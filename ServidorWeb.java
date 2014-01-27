package ServidorWeb;

import java.io.*;
import java.net.*;

/**
 *
 * @author HMIA
 */
public final class ServidorWeb {

    public static void main(String argv[]) throws IOException {
//        if (argv.length != 1) {
//            System.err.println("Formato: ServidorTCP <puerto>");
//            System.exit(-1);
//        }

        ArchivoDeConfiguracion confi = new ArchivoDeConfiguracion();
        Socket cliente = null;
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(confi.getPuerto()); //En el archivo de configuracion buscamos el puerto
            //socket.setSoTimeout(30000); // Establecemos un timeout de 30 segs
            while (true) {
                cliente = socket.accept();   // INVOCAR AL MÉTODO accept() del socket servidor
                SolicitudThread threadServidor = new SolicitudThread(cliente); //Crear un nuevo objeto ThreadServidor 
                threadServidor.start(); //Iniciar la ejecucion del hilo con el método Start()
            }
        } catch (SocketTimeoutException e) {
            System.err.println("30 segs sin recibir nada");
        } catch (Exception e) {
            System.err.println("Error Servidor: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cliente.close();
            socket.close();//Cerramos el socket
        }
    }
}