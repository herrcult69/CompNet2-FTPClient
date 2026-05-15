package herrcult69.compnet;

import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.OutputStream;


// This class stores the methods of socket/ftp related tasks:
// - Server connections and Log in.
// - Sending FTP Commands
// - Handling server responses
// - And helper functions to do the abovemention tasks.

public class FTPCommands {
    private final Properties props = new Properties();

    // For better SOLID, the network related fields were isolated from the GUI.
    private Socket socket;
    private PrintWriter pw;
    private BufferedReader br;

    // Loads server profiles from the embedded config.properties file via the classpath into the Properties map.
     public FTPCommands() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/config.properties")) {
            if (in == null) throw new IOException("config.properties not found");
            props.load(in);
        }
    }

    // Main Method: Connect to the FTP Server, and read the response data.
    public ResponseData connect(String host, int port) throws IOException {
        try {
            socket = new Socket(host, port);
            socket.setSoTimeout(10000); // 10 second timeout for network drops
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            pw = new PrintWriter(socket.getOutputStream(), true);

            ResponseData greet = readReply();
            if (greet != null) {
                System.out.println("Connected, greeting: " + greet.getMessage());
            } else {
                throw new IOException("Connection was established but the server closed it immediately (received null greeting).");
            }
            return greet;
        } catch (IOException e) {
            System.out.println("Connect error: " + e.getMessage());
            cleanup();
            throw e;
        }
    }

    // Helper Method: Read FTP Response from Server. readline returns null if there is problem with connection. We check if the element at index 3 is 'blank' ? Final Line : Not Final
    // We also check if three leading char of the line is Digits (is they status code).
    public ResponseData readReply() throws IOException {
        while (true) {
            String responseString = br.readLine();
            if (responseString == null) {
                return null;
            }
            if (responseString.trim().isEmpty()) {
                continue;
            }
            System.out.println("Response: " + responseString);
            if (responseString.length() >= 4 && responseString.charAt(3) == ' ') {
                if (Character.isDigit(responseString.charAt(0)) &&
                    Character.isDigit(responseString.charAt(1)) &&
                    Character.isDigit(responseString.charAt(2))) {
                    String code = ResponseData.extractStatusCode(responseString);
                    return new ResponseData(code, responseString, null);
                }
            }
        }
    }

    // Helper method: Send the given command via print writer
    public void sendCommand(String command) throws IOException {
        pw.print(command + "\r\n");
        pw.flush();
        System.out.println("Sent command: " + command);
    }
    
    // Main method: Use FTP commands USER + username & PASS + password to log in. Only give the password. 331 means username OKAY need password
    public ResponseData login(String username, String password) throws IOException {
        sendCommand("USER " + username);
        ResponseData reply = readReply();
        if (reply != null && reply.getStatusCode().startsWith("331")) {
            sendCommand("PASS " + password);
            reply = readReply();
        }
        return reply;
    }
    
    // Uses the provided credential from config.properties of the known server's anony users, based on the server ipaddress
    public ResponseData loginAnonymous(String host) throws IOException {
        String profile = resolveProfile(host);
        if (profile == null) {
            throw new IOException("Unknown host: " + host);
        }

        String user = props.getProperty("ftp.profile." + profile + ".user");
        String pass = props.getProperty("ftp.profile." + profile + ".pass");

        return login(user, pass); 
    }

    // Helper function: return the needed profile of given host
    private String resolveProfile(String host) {
        if (host.equals(props.getProperty("ftp.host.local"))) return "local";
        if (host.equals(props.getProperty("ftp.host.rebex"))) return "rebex";
        return null;
    }

    // Send quit command to Server, logout and close the connection
    public ResponseData sendQuit() throws IOException {
        sendCommand("QUIT");
        return readReply();
    }

    // send PWD command to server, get current directory
    public ResponseData sendPWD() throws IOException {
        sendCommand("PWD");
        return readReply();
    }

    // Send CWD command to server to change current working directory.
    public ResponseData sendCWD(String dirPath) throws IOException {
        sendCommand("CWD " + dirPath);
        return readReply();
    }

    // Send MKD command to server to create a new directory.
    public ResponseData sendMKD(String dirName) throws IOException {
        sendCommand("MKD " + dirName);
        return readReply();
    }

    // Send RMD command to server to remove an existing directory.
    public ResponseData sendRMD(String dirName) throws IOException {
        sendCommand("RMD " + dirName);
        return readReply();
    }

    // Send PASV command to server to request data transfer in passive mode. which mean that the server will return a 6 value tuble with ip and port for the new datasocket
    public ResponseData sendPASV() throws IOException {
        sendCommand("PASV");
        return readReply();
    }

    // Send DELE command to server to delete a named file
    public ResponseData sendDELE(String fileName) throws IOException {
        sendCommand("DELE " + fileName);
        return readReply();
    }

    // Send TYPE I command to switch to Image (binary) mode for data transfers instead of A which send Ascii which should only use for TEXT sending
    public ResponseData setTYPEI() throws IOException {
        sendCommand("TYPE I");
        return readReply();
    }

    // Main Method: Open a passive data socket and use RETR to download a file from the server.
    public ResponseData PASV_RETR(String remoteName, String localSavePath) throws IOException {
        Socket dataSocket = openPassiveDataSocket();

        sendCommand("RETR " + remoteName);
        ResponseData resp = readReply();
        if (!resp.isSuccess()) {
            dataSocket.close();
            return resp;
        }

        System.out.println("Downloading");

        try (InputStream is = dataSocket.getInputStream();
             FileOutputStream fos = new FileOutputStream(localSavePath)) {

            byte[] buffer = new byte[4096];
            int numBytesRead;
            while ((numBytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, numBytesRead);
            }
            System.out.println("Download Complete");
        }

        dataSocket.close();
        return readReply();
    }

    // Main Method: Open a passive data socket and use STOR to upload a local file to the server.
    public ResponseData PASV_STOR(String localName, String remoteName) throws IOException {
        Socket dataSocket = openPassiveDataSocket();

        sendCommand("STOR " + remoteName);
        ResponseData resp = readReply();
        if (!resp.isSuccess()) {
            dataSocket.close();
            return resp;
        }

        System.out.println("Uploading: " + localName);

        try (FileInputStream fis = new FileInputStream(localName);
             OutputStream os = dataSocket.getOutputStream()) {

            byte[] buffer = new byte[4096];
            int numBytesRead;
            while ((numBytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, numBytesRead);
            }
            os.flush();
            System.out.println("Upload Complete");
        }

        dataSocket.close();
        return readReply();
    }

    // public ResponseData PASV_LIST() throws IOException {
    //     Socket dataSocket = openPassiveDataSocket();
    //     sendCommand("LIST");
    //     ResponseData resp = readReply();
    //     if (!resp.isSuccess()) {
    //         dataSocket.close();
    //         return resp;
    //     }

    //     System.out.println("-------- LIST DIR ------");
    //     BufferedReader dataReader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));
    //     String dataLine;
    //     while ((dataLine = dataReader.readLine()) != null) {
    //         System.out.println(dataLine);
    //     }
    //     System.out.println("-------------------------");

    //     dataSocket.close();
    //     return readReply();
    // }

    // Main Method: Use LIST on a passive data socket to get directory listing, buffering it for the GUI.
    public ResponseData PASV_LIST_GUI() throws IOException {
        Socket dataSocket = openPassiveDataSocket();
        sendCommand("LIST");
        ResponseData resp = readReply();
        if (!resp.isSuccess()) {
            dataSocket.close();
            return resp;
        }

        StringBuilder sb = new StringBuilder();
        BufferedReader dataReader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));
        String dataLine;
        while ((dataLine = dataReader.readLine()) != null) {
            sb.append(dataLine).append("\n");
        }

        dataSocket.close();
        ResponseData finalResp = readReply();
        finalResp.setData(sb.toString());
        return finalResp;
    }

    // Helper Method: Sends PASV, parses the reply to get IP and port, and opens a new data connection.
    private Socket openPassiveDataSocket() throws IOException {
        sendCommand("PASV");
        ResponseData resp = readReply();
        String line = resp != null ? resp.getMessage() : null;
        if (line == null || !resp.getStatusCode().equals("227")) {
            System.err.println("PASV failed: " + line);
            throw new IOException("PASV failed: " + line);
        }
        InetSocketAddress addr = parsePASV(line);
        return new Socket(addr.getAddress(), addr.getPort());
    }

    // Helper, Parses the 6-value tuple (h1,h2,h3,h4,p1,p2) returned by PASV into an socket addr.
    public InetSocketAddress parsePASV(String line) {
        int i = 4;
        while (i < line.length() && !Character.isDigit(line.charAt(i))) {
            i++;
        }
        if (i >= line.length()) {
            throw new IllegalArgumentException("No digit in PASV reply: " + line);
        }

        int end = line.length() - 1;
        while (end >= 0 && !Character.isDigit(line.charAt(end))) {
            end--;
        }

        if (end <= 0) {
            throw new IllegalArgumentException("BAD PASV reply: " + line);
        }

        String tuplePart = line.substring(i, end + 1);
        String[] octets = tuplePart.split(",");
        if (octets.length != 6) {
            throw new IllegalArgumentException("PASV expected 6 values tuple: " + line);
        }

        int h1 = Integer.parseInt(octets[0].trim());
        int h2 = Integer.parseInt(octets[1].trim());
        int h3 = Integer.parseInt(octets[2].trim());
        int h4 = Integer.parseInt(octets[3].trim());
        int p1 = Integer.parseInt(octets[4].trim());
        int p2 = Integer.parseInt(octets[5].trim());

        String host = h1 + "." + h2 + "." + h3 + "." + h4;
        int port = p1 * 256 + p2;

        return new InetSocketAddress(host, port);
    }

    // Cleanup helper: close all streams and the main command socket when shutting down.
    private void cleanup() {
        try { if (pw != null) pw.close(); } catch (Exception ignore) {}
        try { if (br != null) br.close(); } catch (Exception ignore) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (Exception ignore) {}
    }

    // Main Method: Send QUIT and call cleanup and release resources upon application exit.
    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                sendQuit();
            }
        } catch (IOException e) {
            // ignore, we're closing anyway
        } finally {
            cleanup();
        }
    }
}