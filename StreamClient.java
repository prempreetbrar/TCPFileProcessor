/**
 * StreamClient Class
 *
 * CPSC 441
 * Assignment 1
 *
 */

import java.io.*;
import java.net.Socket;
import java.util.logging.*;

public class StreamClient {

  private static final Logger logger = Logger.getLogger("StreamClient"); // global logger
  private final String SERVER_NAME;
  private final int SERVER_PORT;
  private final int BUFFER_SIZE;

  /**
   * Constructor to initialize the class.
   *
   * @param serverName	remote server name
   * @param serverPort	remote server port number
   * @param bufferSize	buffer size used for read/write
   */
  public StreamClient(String serverName, int serverPort, int bufferSize) {
    this.SERVER_NAME = serverName;
    this.SERVER_PORT = serverPort;
    this.BUFFER_SIZE = bufferSize;
  }

  /**
   * Compress the specified file via the remote server.
   *
   * @param inName		name of the input file to be processed
   * @param outName		name of the output file
   */
  public void getService(int serviceCode, String inName, String outName) {
    final int EOF = -1;
    byte[] buffer = new byte[this.BUFFER_SIZE];
    int numBytes = 0;

    Socket socket = null;
    InputStream inputStream = null;
    OutputStream outputStream = null;
    FileInputStream fileInputStream = null;
    FileOutputStream fileOutputStream = null;

    try {
      socket = new Socket(this.SERVER_NAME, this.SERVER_PORT);
      inputStream = socket.getInputStream();
      outputStream = socket.getOutputStream();

      fileInputStream = new FileInputStream(inName);
      fileOutputStream = new FileOutputStream(outName);

      // assumption: service codes are < 255 and can thus be sent as a byte
      outputStream.write(serviceCode);
      outputStream.flush();

      while ((numBytes = fileInputStream.read(buffer)) != EOF) {
        outputStream.write(buffer, 0, numBytes);
        System.out.println("W " + numBytes);
        outputStream.flush();
      }
      socket.shutdownOutput();

      while ((numBytes = inputStream.read(buffer)) != EOF) {
        System.out.println("R " + numBytes);
        fileOutputStream.write(buffer, 0, numBytes);
        fileOutputStream.flush();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        fileOutputStream.close();
        fileInputStream.close();
        outputStream.close();
        inputStream.close();
        socket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}