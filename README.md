# TCP File Processor

A program that establishes a TCP connection with a remote server, sends a local file to the server for processing, and receives
and stores the processed file in the local file system. Watch a GIF of me interacting with it below!


&nbsp;

## Features
- Server `ECHO`: The server sends back the same data it receives from the client without any modification.
- Server `ZIP`: The server receives data from the client, compresses it using the GZIP format, and sends it back to the client.
- Server `UNZIP`: The server receives GZIP compressed data from the client, decompresses it, and sends it back to the client.
- The client opens a TCP `Socket` to communicate with the server; it first sends the service code provided as a command line argument to the server,
  followed by the input file to be processed. At the same time (ie. simultaneously), it reads the server's processed file chunks from the `Socket` and
  writes them to the output file. The server works analogously in the opposite direction (it reads the regular file from the `Socket`, processes it,
  and writes to the `Socket`).
- The server continues reading from its `Socket` input stream by repeatedly calling the `read()` method until it returns `-1` (indicating the stream is
  at the end of the file). The client signals to the server that transmission is complete (allowing it to stop its reading) by calling `shutdownOutput()`
  to close its side of the connection. The **client can no longer send but can still read the processed data** coming from the server (analogous to sending a segment
  with `FIN = 1`); the client state goes from `ESTAB` -> `FIN_WAIT1` -> `FIN_WAIT2`. See the image below:

![image](https://github.com/prempreetbrar/TCPFileProcessor/assets/89614923/10d15ddb-589d-4544-b915-8aa9eb05ef36)
In the above image, we see that the client can still receive data from the server even after sending a segment with `FIN = 1`. This is exactly what is happening in our 
program; the client closes its side of the connection after the entire file is sent, but continues reading the processed file from the server. 
  
- Client uses a parallel rather than serial design to prevent deadlock; with a serial design, the client would need to transmit its entire file before reading from the server. For very large files, the client would not yet read anything from its `Socket`; the `Socket` buffer would quickly fill up with the server's processed data. This would then block the server when it calls `write()` on the `Socket` output stream (this is the flow control of TCP and is how the `Socket` is designed); a blocked server would not read from its `Socket` input stream, which then blocks the client. Both the client and server would be blocked, leading to a deadlock. The client uses two threads to work around this; one for sending data, and another for reading processed data.

## Usage/Limitations
### When Running the Client:
- `-i <input_file>` specifies the name of the file to be processed by the server; **REQUIRED**
- `-o <output_file>` specifies the name of the processed output file; defaults to `input_file.out`
- `-c <service_code>` specifies the service code requested from the server; `0` is `ECHO`, `1` is `GZIP`, `2` is `UNZIP`. Defaults to `1 (GZIP)`.
- `-b <buffer_size>` specifies the buffer size in _bytes_ used for writing the file to the server and reading processed data. Defaults to `10000`.
- `-p <port_number>` specifies the server's port; default is `2025`.
- `-s <server_name>` specifies the server's hostname; default is `localhost`.

### When Running the Server:
- `-p <port_number>` specifies the server's port; default is `2025`; **the `-p` flags should match when running both the client and server. Otherwise, the client will be unable to connect.**
- `-b <buffer_size>` specifies the buffer size (in bytes) used at the server for read/write operations; default is `1000`. 

## If you want to start up the project on your local machine:
1. Download the code as a ZIP:
<br></br>
![download](https://github.com/prempreetbrar/TCPWebServer/assets/89614923/291dc4a0-fe63-40b8-a70a-8bd3f987d5b6)
&nbsp;

2. Unzip the code:
<br></br>
![unzip](https://github.com/prempreetbrar/TCPWebServer/assets/89614923/e2283434-6b61-41a1-b9b9-bb6380900798)
&nbsp;

3. Open the folder in an IDE, such as VSCode:
<br></br>
![open](https://github.com/prempreetbrar/TCPWebServer/assets/89614923/aa1e0040-15af-4697-b9ab-52104b28e5b4)
&nbsp;

4. Start the server by compiling all files and then running `ServerDriver.java`, as follows:
   ```
   javac *.java
   java ServerDriver -p <port_number> -t <idle_connection_timeout> -r <server_root>
   ```
<br></br>
![server](https://github.com/prempreetbrar/TCPWebServer/assets/89614923/51398c4c-fa7b-4867-b6b9-0b3d40d2bf55)
&nbsp;

5. Send a request to the server using `telnet`, a web browser, or any other application layer protocol:
<br></br>
![request](https://github.com/prempreetbrar/TCPWebServer/assets/89614923/44472d33-d81a-4b1a-a282-0cf861a3d654)


