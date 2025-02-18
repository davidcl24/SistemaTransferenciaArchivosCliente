package org.example;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Application  {
    private static final String DIRECTORY = "clientFiles/";
    private static final Teclado KEYBOARD = new Teclado();
    private static final String ADDRESS = "localhost";
    private static final int PORT = 44444;
    private final byte[] buffer = new byte[16*1024];
    Socket socket;
    InputStream inputStream;
    OutputStream outputStream;
    BufferedReader reader;
    PrintWriter writer;

    public Application() {
        try {
            socket = new Socket(ADDRESS, PORT);
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream));
            writer = new PrintWriter(outputStream, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void showMenu() {
        System.out.println("1) Listar archivos");
        System.out.println("2) Subir archivo");
        System.out.println("3) Descargar archivo");
        System.out.println("0) Salir");
    }

    public int readOption() throws IOException {
        System.out.println("Elige una opcion");
        return KEYBOARD.leerInt();
    }

    public void runOption(int opt) throws IOException {
        switch (opt) {
            case 1:
                writer.println("list"); //se envia el comando "list" al servidor para que sepa que comando ejecutar
                System.out.println("Archivos en el server: ");
                String file;
                while (!(file = reader.readLine()).equals("DONE")) { //lista hasta que recibe "DONE"
                    System.out.println(file);
                }
                break;
            case 2:
                writer.println("upload"); //se envia el comando "upload" al servidor para que sepa que comando ejecutar
                System.out.println("Escriba el nombre del archivo que desea subir: ");
                String fileName = KEYBOARD.leerString();
                writer.println(fileName);
                sendFile(fileName);
                break;
            case 3:
                writer.println("download"); //se envia el comando "download" al servidor para que sepa que comando ejecutar
                System.out.println("Escriba el nombre del archivo que quiere descargar");
                fileName = KEYBOARD.leerString();
                writer.println(fileName);
                recieveFile(fileName);
                break;
            case 0:
                writer.println("END"); //se envia el comando "END" para que el servidor cierre la conexion con este cliente
                System.out.println("Saliendo...");
                socket.close();
                inputStream.close();
                outputStream.close();
                break;
            default:
                System.out.println("Opcion invalida");
        }
    }

    private void sendFile(String fileName) {
        File file = new File(DIRECTORY + fileName);
        if (file.exists()) {
            writer.println(file.length()); //se envia al servidor la longitud del archivo si este existe, para que sepa cuando tiene que terminar de recomponer el archivo
            writer.flush();

            try (FileInputStream inputStream = new FileInputStream(file)){
                int count;
                while ((count = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, count); //se irá mandando el archivo por bytes hasta llegar a final del archivo
                }
                outputStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            writer.println(0); //se envia 0 si no existe, asi el server no recorrerá el archivo
            writer.flush();
            System.out.println("No existe el archivo");
        }
    }

    private void recieveFile(String fileName) {
        File file = new File(DIRECTORY + fileName); //se recibe el nombre del archivo a traves del bufferedReader
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            long fileSize = Long.parseLong(reader.readLine());
            if (fileSize == 0) { //recibimos el nombre del archivo y comprobamos su tamaño, si es 0, termina
                return;
            }

            long totalRead = 0;
            int count;
            while (totalRead < fileSize && (count = inputStream.read(buffer)) != -1) { // se comprobará que el total leido (totalRead) es menor que el tamaño del archivo, y que no hemos llegado al fin del archivo
                fileOutputStream.write(buffer, 0, count); //mientras ambas sean correctas, se escribira el archivo recibido en uno nuevo y se aumentará el total leido
                totalRead += count;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        File directory = new File(DIRECTORY);
        if (!directory.exists())
            directory.mkdir();

        Application app = new Application();
        int opt;
        do {
            app.showMenu();
            opt = app.readOption();
            app.runOption(opt);
            app.writer.flush();
            app.outputStream.flush();
        } while (opt != 0);
    }
}