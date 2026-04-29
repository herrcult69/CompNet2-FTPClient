package herrcult69.compnet;
import java.net.*;
import java.io.*;
import java.util.Scanner;

public class ClientFTP {
    static final int PORT = 21;
    static final String ADDR = "test.rebex.net";

    public static void main(String[] args) {

        try (
            Socket connection = new Socket(ADDR, PORT);
            Scanner sc = new Scanner(System.in);
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            PrintWriter pw = new PrintWriter(connection.getOutputStream(), true);
        ) {
            FTPCommands ftpc = new FTPCommands(connection, pw, br);
                        
            System.out.println("Reading greeting...");
            ftpc.readReply();

            ftpc.loginAnonymous();
            while (true) {
                ftpc.sendPwd();

                ftpc.PASV_LIST();
                System.out.println("> ");
                String input = sc.nextLine().trim();
                if (input.equalsIgnoreCase("baibai")) {
                    break;
                }
                ftpc.sendCWD(input);
            }

            ftpc.sendQuit();

            System.out.println("Done, exiting main");
            
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
