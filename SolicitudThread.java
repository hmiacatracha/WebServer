package ServidorWeb;

import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.activation.MimetypesFileTypeMap;

/**
 *
 * @author HMIA
 */
public class SolicitudThread extends Thread {

    private static Socket socket = null;
    private BufferedReader sEntrada = null;
    private OutputStream sSalida = null;
    private String file;
    private StringBuffer fileToWrite = new StringBuffer(); //String que concatena el mensaje a escribir en el fichero.
    private int codigo;

    public SolicitudThread(Socket s) {
        socket = s;
    }

    @Override
    public void run() {
        try {
            // Inicializamos flujos de datos
            sEntrada = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
            sSalida = socket.getOutputStream();
            byte[] respuesta = null; //bytes sent in response
            String paquete = receiveQuery(); //Received query from BufferedReader
            respuesta = process(paquete); //Process the type of response
            this.reply(respuesta);//Send the response
            this.logFile(respuesta); //Write to File (LOG)
        } catch (SocketTimeoutException e) {
            System.err.println("30 segs sin recibir nada"); //Establish time of 30 minutes.
        } catch (Exception e) {
            System.err.println("Error Solicitud Thread: " + e.getMessage());
        } finally {
            try {
                sSalida.close();
                sEntrada.close();
                socket.close(); //Close this socket
            } catch (IOException ex) {
                System.out.println(ex);
            }
        }
    }

    /**
     *
     * @param sEntrada BufferedReafer, reads text from a character-input stream.
     * @return Query from BufferedReader(Lee el Socket)
     * @throws IOException
     */
    private String receiveQuery() throws IOException {
        String recibido = "";
        String leerSocket = new String();
        do {
            recibido = sEntrada.readLine();
            leerSocket = leerSocket + recibido + "\n";
        } while (!recibido.isEmpty());// Recibimos el mensaje del cliente
        System.out.println("SERVIDOR: Recibido " + leerSocket);
        return leerSocket;
    }

    /**
     *
     * @param paquete Recives a as parameter to process the query
     * @return bytes to send to the output
     * @throws ParseException
     */
    private byte[] process(String paquete) throws ParseException, IOException {
        String method = new String(); //need to known the type of query
        byte[] fileContent = new byte[0]; //Save bytes to send, this is the body of message to send.
        String header = new String(); //Save the header to send, this is the head of message to send, after that build in bytes. 
        List<String> tokens = tokenizar(paquete); //Contains the request (get)
        ArchivoDeConfiguracion conf = new ArchivoDeConfiguracion();

        /**
         * verify type of query that send us.
         *
         */
        if (tokens.contains(
                "GET")) {
            method = "GET";
            file = conf.getDirectory() + tokens.get(tokens.indexOf("GET") + 1); //  0-GET 1-/INDEX
        } else if (tokens.contains(
                "HEAD")) {
            file = conf.getDirectory() + tokens.get(tokens.indexOf("HEAD") + 1);
            method = "HEAD";
        }
        //Verifico que el usuario me haya pedido un archivo  sino le ennvio uno por defecto
        /**
         * verify the type of response to send in bytes: Search the
         * if-Modified-Since in the get, if exist if-Modified-Since we´ll send
         * the response format 304 else We´ll send the requested file Verify the
         * file, if exist we´ll send the file and response format 200 else we´ll
         * send response format 404-not Found
         *
         */
        if (!file.endsWith("/")) {
            File archivo = new File(file);
            if (archivo.exists()) {
                if (tokens.contains("If-Modified-Since:")) {
                    Date fechaMod = new Date(archivo.lastModified());
                    String fechaGetString = tokens.get(tokens.indexOf("If-Modified-Since:") + 1) + " " + tokens.get(tokens.indexOf("If-Modified-Since:") + 2) + " " + tokens.get(tokens.indexOf("If-Modified-Since:") + 3) + " " + tokens.get(tokens.indexOf("If-Modified-Since:") + 4) + " " + tokens.get(tokens.indexOf("If-Modified-Since:") + 5) + " " + tokens.get(tokens.indexOf("If-Modified-Since:") + 6);//Me devuelve la fecha de modificación-
                    SimpleDateFormat formato = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", new Locale("en_EN"));
                    Date fechaGet = formato.parse(fechaGetString);
                    if (fechaMod.after(fechaGet)) {
                        fileContent = readBytes(archivo);
                        codigo = 200;
                        fileToWrite.append("Linea de Petición: ").append(method).append(" ").append(archivo).append("\r\n");
                        fileToWrite.append("Dirección IP: ").append(socket.getInetAddress().toString());
                        header = this.resposeFormat2(codigo, "OK", archivo);
                    } else {
                        codigo = 304;
                        fileToWrite.append("Linea de Petición: ").append(method).append(" ").append(archivo).append("\r\n");
                        fileToWrite.append("Dirección IP: ").append(socket.getInetAddress().toString());
                        header = this.resposeFormat3(codigo, "Not Modified", archivo);
                    }
                } else {
                    fileContent = readBytes(archivo);
                    codigo = 200;
                    fileToWrite.append("Linea de Petición: ").append(method).append(" ").append(archivo).append("\r\n");
                    fileToWrite.append("Dirección IP: ").append(socket.getInetAddress().toString());
                    header = this.resposeFormat2(codigo, "OK", archivo);
                }

            } else {
                codigo = 404;
                fileToWrite.append("Linea de Error: ").append(method).append(" ").append(archivo).append("\r\n");
                fileToWrite.append("Dirección IP: ").append(socket.getInetAddress().toString());
                header = this.resposeFormat4(codigo, "NotFound");
            }
        }//Sino termina en /
        else {
            file = conf.getDirectory() + conf.getDirectoryIndex();
            File fil = new File(file);
            if (!fil.exists()) {
                if (conf.allow().contentEquals("TRUE")) {
                    
                } else {
                }
            }

        }

        /**
         * constructed response, this depends of method, if send GET we´ll send
         * the header and the body but send the HEAD we´ll only send the header.
         */
        if (method.equals("HEAD")) {
            return header.getBytes();

        } else if (method.equals("GET")) {
            byte headerBytes[] = header.getBytes();
            byte[] concatenar = new byte[header.length() + fileContent.length];
            System.arraycopy(headerBytes, 0, concatenar, 0, headerBytes.length);
            System.arraycopy(fileContent, 0, concatenar, headerBytes.length, fileContent.length);
            return concatenar;
        }
        return (new byte[0]);
    }

    /**
     *
     * @param sSalida send bytes output
     * @param respuesta bytes to send
     * @throws IOException
     */
    private void reply(byte[] respuesta) throws IOException {
        System.out.println(new String(respuesta));
        sSalida.write(respuesta, 0, respuesta.length);
        //sSalida.close();
        //System.out.println(respuesta.toString());
    }

    /**
     *
     * @param leerSocket query to process of string type
     * @return List<String>
     */
    private List<String> tokenizar(String leerSocket) {
        List<String> tokenList = new ArrayList<String>();
        String[] array;
        array = leerSocket.split("[ \n]+"); //Splits this string around matches of the given regular expression
        tokenList.addAll(Arrays.asList(array));//Add array to a list
        return tokenList;
    }

    /**
     *
     * @param file
     * @return Get mime type of file
     */
    private static String GetMimeType(File file) {
        return (new MimetypesFileTypeMap().getContentType(file));
    }
    /*
     * Read bytes from input stream (BufferedReader)
     */

    private static byte[] readBytes(File file) {
        try {
            FileInputStream fIn = new FileInputStream(file);
            byte lectura[] = new byte[(int) file.length()];
            fIn.read(lectura);
            return lectura;
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        return null;
    }
    /*
     * Differents format of response to send 
     */

    private String resposeFormat2(int codigo, String mensaje, File archivo) {
        String CRLF = "\r\n";
        Date fechaAc = new Date();
        Date fechaMod = new Date(archivo.lastModified());
        SimpleDateFormat formato = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", new Locale("en_EN"));
        String respuesta = "HTTP/1.0 " + codigo + " " + mensaje + CRLF
                + "Date: " + formato.format(fechaAc) + CRLF
                + "Server: Java/" + "7.2." + CRLF
                + "Last-Modified:" + formato.format(fechaMod) + CRLF
                + "Content-length: " + archivo.length() + CRLF
                + "Content-type: " + GetMimeType(archivo) + CRLF
                + CRLF;
        fileToWrite.append(CRLF);
        fileToWrite.append("Fecha: ").append(formato.format(fechaAc));
        fileToWrite.append(CRLF);
        fileToWrite.append("Codigo de estado: ").append(codigo).append(" ").append(mensaje);
        fileToWrite.append(CRLF);
        return respuesta;
    }

    private String resposeFormat4(int codigo, String mensaje) {
        String CRLF = "\r\n";
        Date fechaAc = new Date();
        SimpleDateFormat formato = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", new Locale("en_EN"));
        String respuesta = "HTTP/1.0 " + codigo + " " + mensaje + CRLF
                + "Date: " + formato.format(fechaAc) + CRLF
                + "Server: Java/" + "7.2." + CRLF
                + CRLF;
        fileToWrite.append(CRLF);
        fileToWrite.append("Fecha: ").append(formato.format(fechaAc));
        fileToWrite.append(CRLF);
        fileToWrite.append("Mensaje de error: ").append("Not-Found: Archivo no econtrado");
        fileToWrite.append(CRLF);
        return respuesta;
    }

    private String resposeFormat3(int codigo, String mensaje, File archivo) {
        String CRLF = "\r\n";
        Date fechaAc = new Date();
        Date fechaMod = new Date(archivo.lastModified());
        SimpleDateFormat formato = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", new Locale("en_EN"));
        String respuesta = "HTTP/1.0 " + codigo + " " + mensaje + CRLF
                + "Date: " + formato.format(fechaAc) + CRLF
                + "Server: Java/" + "7.2." + CRLF
                + "Last-Modified:" + formato.format(fechaMod) + CRLF
                + CRLF;
        fileToWrite.append(CRLF);
        fileToWrite.append("Codigo de estado: ").append(codigo).append(" ").append(mensaje);
        fileToWrite.append(CRLF);
        fileToWrite.append("Fecha: ").append(formato.format(fechaAc));
        fileToWrite.append(CRLF);
        return respuesta;
    }

    private void logFile(byte[] respuesta) {
        try {
            FileWriter fichero = null;
            if ((codigo >= 200) && (codigo < 400)) {
                File accederFile = new File("AccederFile.txt");
                fichero = new FileWriter(accederFile, true);
                fileToWrite.append("Tamaño bytes: ").append(respuesta.length);
                fileToWrite.append("\r\n");
                fileToWrite.append("\r\n");
                BufferedWriter pw = new BufferedWriter(fichero);
                pw.write(fileToWrite.toString());
                pw.newLine();
                pw.close();
                fileToWrite = null;
            } else if ((codigo >= 400) && (codigo < 500)) {
                File errorFile = new File("ErrorFile.txt");
                fichero = new FileWriter(errorFile, true);
                BufferedWriter pw = new BufferedWriter(fichero);
                pw.write(fileToWrite.toString());
                pw.newLine();
                pw.close();
                fileToWrite = null;
            }
        } catch (IOException ex) {
            System.out.println("ERROR EN EL FICHERO");
        }
    }
}