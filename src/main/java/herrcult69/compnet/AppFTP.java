package herrcult69.compnet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;
import java.io.File;

public class AppFTP extends Application {
    private FTPCommands ftpc;
    private Socket socket;

    private TextField hostField;
    private TextField userField;
    private PasswordField passField;
    private TextArea clientLog;
    private TextArea serverLog;

    private Button connectButton;
    private Button loginButton;
    private Button anonLoginButton;

    Button pwdBtn = new Button("PWD");
    Button lsBtn = new Button("LS");
    Button cdBtn = new Button("CD...");
    Button mkdirBtn = new Button("MKDIR...");
    Button putBtn = new Button("Upload (PUT)...");
    Button getBtn = new Button("Download (GET)...");

    private HBox commandMenu;
    private Button logoutButton;

    private boolean isConnected = false;
    private boolean isLoggedIn = false;

    @Override
    public void start(Stage stage) {
        // Row 1: Host
        hostField = new TextField("test.rebex.net");
        hostField.setPromptText("Host");
        hostField.setPrefColumnCount(20);
        HBox row1 = new HBox(5, hostField);
        row1.setPrefHeight(30);

        // Row 2: User
        userField = new TextField("demo");
        userField.setPromptText("User");
        userField.setPrefColumnCount(20);
        HBox row2 = new HBox(5, userField);
        row2.setPrefHeight(30);

        // Row 3: Password
        passField = new PasswordField();
        passField.setPromptText("Password");
        passField.setPrefColumnCount(20);
        HBox row3 = new HBox(5, passField);
        row3.setPrefHeight(30);

        // Left side: VBox with the 3 rows
        VBox leftBox = new VBox(5, row1, row2, row3);
        leftBox.setPrefWidth(300);

        // Buttons
        connectButton = new Button("Connect");
        connectButton.setPrefWidth(150);
        connectButton.setPrefHeight(80);

        loginButton = new Button("Login");
        loginButton.setPrefWidth(150);
        loginButton.setPrefHeight(80);

        anonLoginButton = new Button("Anonymous Login");
        anonLoginButton.setPrefWidth(150);
        anonLoginButton.setPrefHeight(80);

        logoutButton = new Button("Log Out");
        logoutButton.setPrefWidth(150);
        logoutButton.setPrefHeight(80);

        // Top area
        HBox topPanel = new HBox(10, leftBox, connectButton, loginButton, anonLoginButton, logoutButton);
        topPanel.setPrefHeight(120);

        // Center: logs
        clientLog = new TextArea();
        clientLog.setEditable(false);
        clientLog.setPrefRowCount(20);
        clientLog.setPrefColumnCount(40);
        clientLog.setPromptText("Client commands / actions");

        serverLog = new TextArea();
        serverLog.setEditable(false);
        serverLog.setPrefRowCount(20);
        serverLog.setPrefColumnCount(40);
        serverLog.setPromptText("Server replies / listings");

        SplitPane centerPane = new SplitPane(clientLog, serverLog);
        centerPane.setDividerPositions(0.5);
        
        commandMenu = new HBox(10, pwdBtn, lsBtn, cdBtn, putBtn, getBtn);

        BorderPane root = new BorderPane();
        root.setTop(topPanel);
        root.setCenter(centerPane);
        root.setBottom(commandMenu);

        Scene scene = new Scene(root, 900, 600);
        stage.setTitle("FTP Client (JavaFX)");
        stage.setScene(scene);
        stage.show();


        // Initial UI state:
        loginButton.setDisable(true);
        anonLoginButton.setDisable(true);
        logoutButton.setDisable(true);
        userField.setDisable(true);
        passField.setDisable(true);
        commandMenu.setDisable(true);

        // Wire buttons
        connectButton.setOnAction(e -> onConnectButtonClicked());
        loginButton.setOnAction(e -> onLoginButtonClicked());
        anonLoginButton.setOnAction(e -> onAnonymousLoginClicked());
        logoutButton.setOnAction(e -> onLogoutButtonClicked());

        pwdBtn.setOnAction(e -> {
            clientLog.appendText("> pwd\n");
            try {
                if (ftpc != null) serverLog.appendText(ftpc.sendPWD() + "\n");
            } catch (IOException ex) {
                clientLog.appendText("Error: " + ex.getMessage() + "\n");
            }
        });

        lsBtn.setOnAction(e -> {
            clientLog.appendText("> ls\n");
            try {
                if (ftpc != null) {
                    String listResult = ftpc.PASV_LIST_GUI();
                    serverLog.appendText(listResult + "\n");
                }
            } catch (Exception ex) {
                clientLog.appendText("Error: " + ex.getMessage() + "\n");
            }
        });

        cdBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Change Directory");
            dialog.setHeaderText("Enter directory path:");
            dialog.setContentText("Path:");
            dialog.showAndWait().ifPresent(path -> {
                if (!path.trim().isEmpty()) {
                    clientLog.appendText("> cd " + path + "\n");
                    try {
                        if (ftpc != null) serverLog.appendText(ftpc.sendCWD(path) + "\n");
                    } catch (IOException ex) {
                        clientLog.appendText("Error: " + ex.getMessage() + "\n");
                    }
                }
            });
        });

        putBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select File to Upload");
            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                String absPath = selectedFile.getAbsolutePath();
                String name = selectedFile.getName();
                clientLog.appendText("> put " + absPath + "\n");
                try {
                    if (ftpc != null) {
                        ftpc.PASV_STOR(absPath, name);
                        serverLog.appendText("Upload completed for " + name + "\n");
                    }
                } catch (IOException ex) {
                    clientLog.appendText("Error: " + ex.getMessage() + "\n");
                }
            }
        });

        getBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Download File");
            dialog.setHeaderText("Enter remote file name to download:");
            dialog.setContentText("File name:");
            dialog.showAndWait().ifPresent(fileName -> {
                if (!fileName.trim().isEmpty()) {
                    clientLog.appendText("> get " + fileName + "\n");
                    try {
                        if (ftpc != null) {
                            ftpc.PASV_RETR(fileName);
                            serverLog.appendText("Download completed for " + fileName + "\n");
                        }
                    } catch (IOException ex) {
                        clientLog.appendText("Error: " + ex.getMessage() + "\n");
                    }
                }
            });
        });
    }

    private void onConnectButtonClicked() {
        if (ftpc != null) {
            clientLog.appendText("Already connected.\n");
            return;
        }

        String host = hostField.getText().trim();
        if (host.isEmpty()) {
            clientLog.appendText("Host is required.\n");
            hostField.setStyle("-fx-border-color: red;");
            return;
        } else {
            hostField.setStyle("");
        }

        try {
            socket = new Socket(host, 21);
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
            ftpc = new FTPCommands(pw, br, socket);

            clientLog.appendText("Connected to " + host + ".\n");

            // Read server greeting
            String greet = ftpc.readReply();
            serverLog.appendText(greet + "\n");

            // Update UI: allow login, edit credentials
            connectButton.setDisable(true);
            loginButton.setDisable(false);
            anonLoginButton.setDisable(false);
            logoutButton.setDisable(false); // Enable logout once connected
            userField.setDisable(false);
            passField.setDisable(false);
            

        } catch (IOException ex) {
            clientLog.appendText("Connection failed: " + ex.getMessage() + "\n");
            cleanupConnection();
        }
    }

    private void onLoginButtonClicked() {
        if (ftpc == null) {
            clientLog.appendText("Not connected.\n");
            return;
        }

        String user = userField.getText().trim();
        String pass = passField.getText();

        if (user.isEmpty()) {
            clientLog.appendText("User is required.\n");
            userField.setStyle("-fx-border-color: red;");
            return;
        } else {
            userField.setStyle("");
        }

        try {
            String reply = ftpc.login(user, pass); // adjust to your FTPCommands API
            serverLog.appendText(reply + "\n");

            int code = Integer.parseInt(reply.substring(0, 3));
            if (code == 230) {
                clientLog.appendText("Login successful as " + user + ".\n");
                enableCommandsUI(true);
                loginButton.setDisable(true);
                anonLoginButton.setDisable(true);
            } else {
                clientLog.appendText("Login failed (code " + code + ").\n");
            }
        } catch (IOException ex) {
            clientLog.appendText("Login error: " + ex.getMessage() + "\n");
            cleanupConnection();
        }
    }

    private void onAnonymousLoginClicked() {
        if (ftpc == null) {
            clientLog.appendText("Not connected.\n");
            return;
        }

        String anonUser = "demo";
        String anonPass = "password"; // or any email-like string

        try {
            String reply = ftpc.login(anonUser, anonPass); // adjust to your API
            serverLog.appendText(reply + "\n");

            int code = Integer.parseInt(reply.substring(0, 3));
            if (code == 230) {
                clientLog.appendText("Anonymous login successful.\n");
                enableCommandsUI(true);
            } else {
                clientLog.appendText("Anonymous login failed (code " + code + ").\n");
            }
        } catch (IOException ex) {
            clientLog.appendText("Anonymous login error: " + ex.getMessage() + "\n");
            cleanupConnection();
        }
    }

    private void onLogoutButtonClicked() {
        clientLog.appendText("Disconnecting...\n");
        cleanupConnection();
    }

    private void cleanupConnection() {
        try {
            if (ftpc != null) {
                ftpc.sendQuit(); // Send QUIT command politely
            }
        } catch (Exception ignore) {}

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception ignore) {}

        ftpc = null;
        socket = null;

        connectButton.setDisable(false);
        loginButton.setDisable(true);
        anonLoginButton.setDisable(true);
        logoutButton.setDisable(true);
        userField.setDisable(true);
        passField.setDisable(true);
        enableCommandsUI(false);
    }

    private void enableCommandsUI(boolean enable) {
        if (commandMenu != null) {
            commandMenu.setDisable(!enable);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}