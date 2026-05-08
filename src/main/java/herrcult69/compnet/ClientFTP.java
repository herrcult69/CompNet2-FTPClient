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
            FTPCommands ftpc = new FTPCommands(pw, br, connection);
                        
            System.out.println("Reading greeting...");
            ftpc.readReply();

            ftpc.loginAnonymous();
            System.out.println("Commands: pwd, ls, cd <dir>, get <file>, put <local> <remote>, quit");
            
            System.out.println("Setting to Binary Mode ...");
            ftpc.setTYPEI();
            while (true) {
                System.out.print("ftp> ");
                String input = sc.nextLine().trim();
                if (input.isEmpty()) continue;

                String[] parts = input.split("\\s+");
                String cmd = parts[0].toLowerCase();

                if (cmd.equals("quit") || cmd.equals("exit") || cmd.equals("/baibai")) {
                    break;
                } else if (cmd.equals("pwd")) {
                    ftpc.sendPWD();
                } else if (cmd.equals("ls")) {
                    ftpc.PASV_LIST();
                } else if (cmd.equals("cd")) {
                    if (parts.length > 1) {
                        ftpc.sendCWD(parts[1]);
                    } else {
                        System.out.println("Usage: cd <dir>");
                    }
                } else if (cmd.equals("get")) {
                    if (parts.length > 1) {
                        ftpc.PASV_RETR(parts[1]);
                    } else {
                        System.out.println("Usage: get <remote_file>");
                    }
                } else if (cmd.equals("put")) {
                    if (parts.length > 2) {
                        ftpc.PASV_STOR(parts[1], parts[2]);
                    } else {
                        System.out.println("Usage: put <local_file> <remote_file>");
                    }
                } else {
                    System.out.println("Unknown command.");
                }
            }

            ftpc.sendQuit();

            System.out.println("Done, exiting main");
            
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
