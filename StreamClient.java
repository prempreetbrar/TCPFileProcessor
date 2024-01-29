/**
 * StreamClient Class
 *
 * CPSC 441 - L01 - T01
 * Assignment 1
 * 
 * TA: Amir Shani
 * Student: Prempreet Brar
 * UCID: 30112576
 *
 * This class initiates a TCP connection to a remote server, reads the contents of a given file,
 * sends it to the server for processing, receives the processed file content, and then saves it to
 * a new file in the local file system.
 */

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.*;

public class StreamClient {

    private static final Logger logger = Logger.getLogger("StreamClient"); // global logger
    /*
     * Static because EOF, SUCCESSFUL_TERMINATION, OFFSET is the same regardless of the StreamClient.
     * Our offset is 0 because we don't want to skip any bytes in the buffer.
     */
    private static final int EOF = -1;
    private static final int SUCCESSFUL_TERMINATION = 0;
    private static final int UNSUCCESSFUL_TERMINATION = -1;
    private static final int OFFSET = 0;
    private final int BUFFER_SIZE;

    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    /**
     * Constructor to initialize the class.
     *
     * @param serverName remote server name
     * @param serverPort remote server port number
     * @param bufferSize buffer size used for read/write
     */
    public StreamClient(String serverName, int serverPort, int bufferSize) {
        this.BUFFER_SIZE = bufferSize;
        boolean wasSuccessful = false;

        try {
            socket = new Socket(serverName, serverPort);
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            wasSuccessful = true;
        } /*
         * We catch UnknownHostException first because it is a subclass of IOException. Of course,
         * the behaviour in both catch blocks is identical (printStackTrace), but if we wanted different
         * behaviour in the future, having these different blocks is good practice.
         */
        catch (UnknownHostException e) {
            e.printStackTrace();
        } 
        catch (IOException e) {
            e.printStackTrace();
        } 
        finally {
            if (!wasSuccessful) {
                closeGracefully(outputStream, inputStream, socket);
                System.exit(UNSUCCESSFUL_TERMINATION);
            }
        }
    }

    /**
     * Join all opened threads (wait for them to finish executing).
     *
     * @param threads all threads which need to finish execution.
     */
    private void joinGracefully(Thread... threads) {
        /*
         * We need to surround this with a try-catch block because the joining itself can raise
         * an InterruptedException. In this case, if joining fails, there is nothing else we can do. We must also
         * ensure that the thread is not null. This is because other parts of the program instantiate a Thread
         * variable to null before reassignment.
         */
        try {
            for (Thread thread : threads) {
                if (thread != null) {
                    thread.join();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Close all opened streams, sockets, and other resources before terminating the program.
     *
     * @param resources all resources which need to be closed
     */
    private void closeGracefully(Closeable... resources) {
        /*
         * We need to surround this with a try-catch block because the closing itself can raise
         * an IOException. In this case, if closing fails, there is nothing else we can do. We must also
         * ensure the resource is not null. This is because other parts of the program instantiate certain
         * resources to null before reassignment.
         */
        try {
            for (Closeable resource : resources) {
                if (resource != null) {
                    resource.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Compress the specified file via the remote server.
     *
     * @param inName  name of the input file to be processed
     * @param outName name of the output file
     */
    public void getService(int serviceCode, String inName, String outName) {
        if (socket == null || inputStream == null || outputStream == null) {
            return;
        }
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        Thread sendFileToServer = null;
        Thread readFileFromServer = null;
        boolean wasSuccessful = true;

        try {
            fileInputStream = new FileInputStream(inName);
            fileOutputStream = new FileOutputStream(outName);

            /* 
                assumption: service codes are < 255 and can thus be sent as a byte. We
                flush to ensure the server received the service code before we proceed.
            */
            outputStream.write(serviceCode);
            outputStream.flush();

            sendFileToServer = sendFileToServer(fileInputStream);
            readFileFromServer = readFileFromServer(fileOutputStream);
            sendFileToServer.start();
            readFileFromServer.start();
        } 
        /*
         * IOException from writing serviceCode. Thread exceptions are caught in their respective
         * run methods. 
         */
        catch (IOException e) {
            e.printStackTrace();
            wasSuccessful = false;
        } 
        finally {
            joinGracefully(sendFileToServer, readFileFromServer);
            closeGracefully(
                fileOutputStream,
                fileInputStream,
                outputStream,
                inputStream,
                socket
            );
            if (wasSuccessful) {
                System.exit(SUCCESSFUL_TERMINATION);
            } 
            else {
                System.exit(UNSUCCESSFUL_TERMINATION);
            }
        }
    }

    /**
     * Sends the file to the server via a separate thread.
     *
     * @param fileInputStream  name of the stream being used to read the file.
     * @return a thread that will perform the entire process.
     */
    private Thread sendFileToServer(FileInputStream fileInputStream) {
        Thread sendFileToServer = new Thread() {
            public void run() {
                /*
                 * The numBytes tells us how many bytes to actually write to the stream; this may
                 * be different from the buffer size (ie. if the number of bytes remaining is <
                 * buffer.length). This is why we cannot specify buffer.length as the number of bytes being written,
                 * as we would get an IndexOutOfBounds exception when we reach the end.
                 */
                int numBytes = 0;
                byte[] buffer = new byte[StreamClient.this.BUFFER_SIZE];

                try {
                    while ((numBytes = fileInputStream.read(buffer)) != EOF) {
                        outputStream.write(buffer, OFFSET, numBytes);
                        System.out.println("W " + numBytes);
                        outputStream.flush();
                    }
                    // signal to server that file transmission is complete
                    socket.shutdownOutput();
                } 
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        return sendFileToServer;
    }

    /**
     * Reads the processed file bytes from the server (that have had the service performed on them).
     *
     * @param fileOutputStream  name of the stream being used to output the file to directory
     * @return                  a thread that will execute the reading
     */
    private Thread readFileFromServer(FileOutputStream fileOutputStream) {
        Thread readFileFromServer = new Thread() {
            public void run() {
                /*
                 * The numBytes tells us how many bytes to actually read from the stream; this may
                 * be different from the buffer size (ie. if the number of bytes remaining is <
                 * buffer.length). This is why we cannot specify buffer.length as the number of bytes being read,
                 * as we would get an IndexOutOfBounds exception when we reach the end.
                 */
                int numBytes = 0;
                byte[] buffer = new byte[StreamClient.this.BUFFER_SIZE];

                try {
                    while ((numBytes = inputStream.read(buffer)) != EOF) {
                        System.out.println("R " + numBytes);
                        fileOutputStream.write(buffer, OFFSET, numBytes);
                    }
                    /*
                     * we do not need the bytes to be written to the file as we are reading it; this is because the concurrency
                     * and interaction between client and server is related to them sending the file to each other. The server
                     * already flushes (as specified by Dr. Ghaderi), and we have flushed on the client side when writing. There is no
                     * urgency when outputting to file.
                     * 
                     * We can comfortably flush at the end.
                     */
                    fileOutputStream.flush();
                } 
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        return readFileFromServer;
    }
}
