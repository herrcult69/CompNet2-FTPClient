package herrcult69.compnet;
import java.net.*;
import java.io.*;

public class ClientFTP {
    static final int PORT = 21;
    static final String ADDR = "ftp.gnu.org";

    public static void main(String[] args) {
        try (
            Socket connection = new Socket(ADDR, PORT);
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            PrintWriter pw = new PrintWriter(connection.getOutputStream());
        ) {
            String responseString = br.readLine().trim();
            System.out.println("Response: " + responseString);
            
        } catch (Exception e) {
            e.getMessage();
        }
    }
    
}
