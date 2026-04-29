package herrcult69.compnet;

import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class FTPCommands {
    private final Socket socket;
    private final PrintWriter pw;
    private final BufferedReader br;

    public FTPCommands(Socket socket, PrintWriter pw, BufferedReader br) {
        this.socket = socket;
        this.pw = pw;
        this.br = br;
    }

    public String readReply() throws IOException { // return last line
        while (true) {
            String responseString = br.readLine();
            if (responseString == null) {
                return null;
            }
            if (responseString.trim().isEmpty()) {
                continue; // Ignore empty lines, or you can break/return based on preference
            }
            System.out.println("Response: " + responseString);
            if (responseString.length() >= 4 && responseString.charAt(3) == ' ') {
                if (Character.isDigit(responseString.charAt(0)) &&
                        Character.isDigit(responseString.charAt(1)) &&
                        Character.isDigit(responseString.charAt(2))) {
                    return responseString;
                }
            }
        }
    }

    public void sendCommand(String command) throws IOException {
        pw.print(command + "\r\n");
        pw.flush();
        System.out.println("Sent command: " + command);
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

    public boolean loginAnonymous() throws IOException {
        sendCommand("USER demo");
        String reply = readReply();
        if (reply != null && reply.startsWith("331")) {
            sendCommand("PASS password");
            reply = readReply();
        }
        return reply != null && reply.startsWith("230");
    }

    public void sendQuit() throws IOException {
        sendCommand("QUIT");
        readReply();
    }

    public void sendPwd() throws IOException {
        sendCommand("PWD");
        readReply();
    }

    public void sendCWD(String dirPath) throws IOException {
        sendCommand("CWD " + dirPath);
        readReply();
    }

    public void sendMKD(String dirName) throws IOException {
        sendCommand("MKD " + dirName);
        readReply();
    }

    public void sendRMD(String dirName) throws IOException {
        sendCommand("RMD " + dirName);
        readReply();
    }

    public void sendPASV() throws IOException {
        sendCommand("PASV");
        readReply();
    }

    public void PASV_LIST() throws IOException {
        sendCommand("PASV");
        String line = readReply();
        if (line == null || !line.startsWith("227")) {
            System.err.println("PASV failed: " + line);
            return;
        }

        InetSocketAddress socketAddr = parsePASV(line);
        Socket dataSocket = connectDataSocket(socketAddr);

        sendCommand("LIST");
        readReply();

        // Read directory structure from the DATA socket
        System.out.println("-------- LIST DIR ------");
        BufferedReader dataReader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));
        String dataLine;
        while ((dataLine = dataReader.readLine()) != null) {
            System.out.println(dataLine);
        }
        System.out.println("-------------------------");

        closeDataSocket(dataSocket);

        // Read the 226 Transfer complete from the CONTROL socket
        readReply();
    }

    public Socket connectDataSocket(InetSocketAddress socketAddress) throws IOException {
        Socket sc = new Socket(socketAddress.getAddress(), socketAddress.getPort());
        return sc;
    }

    public void closeDataSocket(Socket socket) throws IOException {
        socket.close();
    }

}
