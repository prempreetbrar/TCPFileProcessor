/**
 * StreamClient Class
 *
 * CPSC 441
 * Assignment 1
 * Prempreet Brar
 * UCID: 30112576
 *
 * This class initiates a TCP connection to a remote server, reads the contents of a given file,
 * sends it to the server for processing, receives the processed file content, and then saves it to
 * a new file in the local file system.
 */

import java.io.*;
import java.net.Socket;
import java.util.logging.*;

public class StreamClient {

    private static final Logger logger = Logger.getLogger("StreamClient"); // global logger
    private static final int EOF = -1;
    private final String SERVER_NAME;
    private final int SERVER_PORT;
    private final int BUFFER_SIZE;

    /**
     * Constructor to initialize the class.
     *
     * @param serverName remote server name
     * @param serverPort remote server port number
     * @param bufferSize buffer size used for read/write
     */
    public StreamClient(String serverName, int serverPort, int bufferSize) {
        this.SERVER_NAME = serverName;
        this.SERVER_PORT = serverPort;
        this.BUFFER_SIZE = bufferSize;
    }

    /**
     * Compress the specified file via the remote server.
     *
     * @param inName  name of the input file to be processed
     * @param outName name of the output file
     */
    public void getService(int serviceCode, String inName, String outName) {
        Socket socket = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        Thread sendFileToServer = null;
        Thread readFileFromServer = null;

        try {
            socket = new Socket(SERVER_NAME, SERVER_PORT);
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            fileInputStream = new FileInputStream(inName);
            fileOutputStream = new FileOutputStream(outName);

            // assumption: service codes are < 255 and can thus be sent as a byte
            outputStream.write(serviceCode);
            outputStream.flush();

            sendFileToServer =
                sendFileToServer(fileInputStream, socket, outputStream);
            readFileFromServer =
                readFileFromServer(inputStream, fileOutputStream);
            sendFileToServer.start();
            readFileFromServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                sendFileToServer.join();
                readFileFromServer.join();

                fileOutputStream.close();
                fileInputStream.close();
                outputStream.close();
                inputStream.close();
                socket.close();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private Thread sendFileToServer(
        FileInputStream fileInputStream,
        Socket socket,
        OutputStream outputStream
    ) {
        Thread sendFileToServer = new Thread() {
            public void run() {
                int numBytes = 0;
                byte[] buffer = new byte[StreamClient.this.BUFFER_SIZE];

                try {
                    while ((numBytes = fileInputStream.read(buffer)) != EOF) {
                        outputStream.write(buffer, 0, numBytes);
                        System.out.println("W " + numBytes);
                        outputStream.flush();
                    }
                    socket.shutdownOutput();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        return sendFileToServer;
    }

    private Thread readFileFromServer(
        InputStream inputStream,
        FileOutputStream fileOutputStream
    ) {
        Thread readFileFromServer = new Thread() {
            public void run() {
                int numBytes = 0;
                byte[] buffer = new byte[StreamClient.this.BUFFER_SIZE];

                try {
                    while ((numBytes = inputStream.read(buffer)) != EOF) {
                        System.out.println("R " + numBytes);
                        fileOutputStream.write(buffer, 0, numBytes);
                    }
                    fileOutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        return readFileFromServer;
    }
}
