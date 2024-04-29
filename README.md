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
  with `FIN = 1`; the client state goes from `ESTAB` -> `FIN_WAIT1` -> `FIN_WAIT2`. See the image below:

![image](https://github.com/prempreetbrar/TCPFileProcessor/assets/89614923/10d15ddb-589d-4544-b915-8aa9eb05ef36)
In the above image, we see that the client can still receive data from the server even after sending a segment with `FIN = 1`. This is exactly what is happening in our 
program; the client closes its side of the connection after the entire file is sent, but continues reading the processed file from the server. 
  
- 

- To prevent non-responsive clients from hogging server resources, if the server does not receive an HTTP message from the
  client after the initial 3-way handshake, the server closes the connection and sends an error message with status code `408`. Note that
  this only occurs if the client is connecting using `telnet` or certain other application layer protocols. With something like a browser, the
  handshake is automatically (ie. _implicitly_) followed by an HTTP request.
- After server is shutdown, waits reasonable amount of time for current requests to be serviced before terminating.
- If the server can recover from exceptions (ie. the exception only affected a worker thread), then the server continues with normal execution. Otherwise,
  it terminates. 

## Usage/Limitations
- The root directory of the web server, where the objects are located, is specified as a command line input parameter. If the object path in
  the `GET` request is `/object-path`, then the file containing the object is located on the absolute path `server-root/object-path` in the file
  system, where `server-root` is the root directory of the web server.
- `-p <port_number>` specifies the server's port; default is `2025`
- `-t <idle_connection_timeout>` specifies the time after which the server closes the TCP connection in **milli-seconds**; default is `0` (which means infinity,
   ie. idle connections are not closed)
- `-r <server-root>` is the root directory of the web server (where all its HTTP objects are located); default is the current directory (directory in which program
   is ran)
- `quit` is typed in the system terminal to shut the server down. 
- Sends responses with HTTP version `HTTP/1.1`; only returns responses with the following status codes/phrases:
  ```
  200 OK
  400 Bad Request
  404 Not Found
  408 Request Timeout
  ```
- Assumes all header lines in the HTTP request are formatted correctly; only an error in the HTTP request line can trigger a `400 Bad Request` error. A
  properly formatted request line consists of three _mandatory_ parts which are separated by one or more spaces, as follows: `GET /object-path HTTP/1.1`.
  The command `GET` and protocol `HTTP/1.1` are fixed, while the `object-path` is optional. If no `object-path` is provided, _ie._ the request only specifies "/",
  then `index.html` is assumed by default. 

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


