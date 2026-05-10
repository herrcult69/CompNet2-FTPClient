package herrcult69.compnet;

import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.OutputStream;

public class FTPCommands {
    private Socket socket;
    private PrintWriter pw;
    private BufferedReader br;

    public ResponseData connect(String host, int port) throws IOException {
        try {
            socket = new Socket(host, port);
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            pw = new PrintWriter(socket.getOutputStream(), true);

            ResponseData greet = readReply();
            System.out.println("Connected, greeting: " + greet.getMessage());
            return greet;
        } catch (IOException e) {
            System.out.println("Connect error: " + e.getMessage());
            cleanup();
            throw e;
        }
    }

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

    public void sendCommand(String command) throws IOException {
        pw.print(command + "\r\n");
        pw.flush();
        System.out.println("Sent command: " + command);
    }

    public ResponseData loginAnonymous() throws IOException {
        sendCommand("USER demo");
        ResponseData reply = readReply();
        if (reply != null && reply.getStatusCode().startsWith("331")) {
            sendCommand("PASS password");
            reply = readReply();
        }
        return reply;
    }

    public ResponseData login(String username, String password) throws IOException {
        sendCommand("USER " + username);
        ResponseData reply = readReply();
        if (reply != null && reply.getStatusCode().startsWith("331")) {
            sendCommand("PASS " + password);
            reply = readReply();
        }
        return reply;
    }

    public ResponseData sendQuit() throws IOException {
        sendCommand("QUIT");
        return readReply();
    }

    public ResponseData sendPWD() throws IOException {
        sendCommand("PWD");
        return readReply();
    }

    public ResponseData sendCWD(String dirPath) throws IOException {
        sendCommand("CWD " + dirPath);
        return readReply();
    }

    public ResponseData sendMKD(String dirName) throws IOException {
        sendCommand("MKD " + dirName);
        return readReply();
    }

    public ResponseData sendRMD(String dirName) throws IOException {
        sendCommand("RMD " + dirName);
        return readReply();
    }

    public ResponseData sendPASV() throws IOException {
        sendCommand("PASV");
        return readReply();
    }

    public ResponseData sendDELE(String fileName) throws IOException {
        sendCommand("DELE " + fileName);
        return readReply();
    }

    public ResponseData setTYPEI() throws IOException {
        sendCommand("TYPE I");
        return readReply();
    }

    public ResponseData PASV_RETR(String remoteName) throws IOException {
        Socket dataSocket = openPassiveDataSocket();

        sendCommand("RETR " + remoteName);
        ResponseData resp = readReply();
        if (!resp.isSuccess()) {
            closeDataSocket(dataSocket);
            return resp;
        }

        System.out.println("Downloading");

        try (InputStream is = dataSocket.getInputStream();
             FileOutputStream fos = new FileOutputStream(remoteName)) {

            byte[] buffer = new byte[4096];
            int numBytesRead;
            while ((numBytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, numBytesRead);
            }
            System.out.println("Download Complete");
        }

        closeDataSocket(dataSocket);
        return readReply();
    }

    public ResponseData PASV_STOR(String localName, String remoteName) throws IOException {
        Socket dataSocket = openPassiveDataSocket();

        sendCommand("STOR " + remoteName);
        ResponseData resp = readReply();
        if (!resp.isSuccess()) {
            closeDataSocket(dataSocket);
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

        closeDataSocket(dataSocket);
        return readReply();
    }

    // public ResponseData PASV_LIST() throws IOException {
    //     Socket dataSocket = openPassiveDataSocket();
    //     sendCommand("LIST");
    //     ResponseData resp = readReply();
    //     if (!resp.isSuccess()) {
    //         closeDataSocket(dataSocket);
    //         return resp;
    //     }

    //     System.out.println("-------- LIST DIR ------");
    //     BufferedReader dataReader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));
    //     String dataLine;
    //     while ((dataLine = dataReader.readLine()) != null) {
    //         System.out.println(dataLine);
    //     }
    //     System.out.println("-------------------------");

    //     closeDataSocket(dataSocket);
    //     return readReply();
    // }

    public ResponseData PASV_LIST_GUI() throws IOException {
        Socket dataSocket = openPassiveDataSocket();
        sendCommand("LIST");
        ResponseData resp = readReply();
        if (!resp.isSuccess()) {
            closeDataSocket(dataSocket);
            return resp;
        }

        StringBuilder sb = new StringBuilder();
        BufferedReader dataReader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));
        String dataLine;
        while ((dataLine = dataReader.readLine()) != null) {
            sb.append(dataLine).append("\n");
        }

        closeDataSocket(dataSocket);
        ResponseData finalResp = readReply();
        finalResp.setData(sb.toString());
        return finalResp;
    }

    private Socket openPassiveDataSocket() throws IOException {
        sendCommand("PASV");
        ResponseData resp = readReply();
        String line = resp != null ? resp.getMessage() : null;
        if (line == null || !resp.getStatusCode().equals("227")) {
            System.err.println("PASV failed: " + line);
            throw new IOException("PASV failed: " + line);
        }
        InetSocketAddress addr = parsePASV(line);
        return connectDataSocket(addr);
    }

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
            throw new IllegalArgumentException("PASV expected 6 values tuple from: " + line);
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

    private Socket connectDataSocket(InetSocketAddress socketAddress) throws IOException {
        return new Socket(socketAddress.getAddress(), socketAddress.getPort());
    }

    private void closeDataSocket(Socket socket) throws IOException {
        socket.close();
    }

    // Cleanup helper
    private void cleanup() {
        try { if (pw != null) pw.close(); } catch (Exception ignore) {}
        try { if (br != null) br.close(); } catch (Exception ignore) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (Exception ignore) {}
    }

    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                sendCommand("QUIT");
                readReply();
            }
        } catch (IOException e) {
            // ignore, we're closing anyway
        } finally {
            cleanup();
        }
    }
}