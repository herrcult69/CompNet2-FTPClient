package herrcult69.compnet;

import java.io.IOException;

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

    private TextField hostField;
    private TextField userField;
    private PasswordField passField;
    private TextArea clientLog;
    private TextArea serverLog;

    private Button connectButton;
    private Button loginButton;
    private Button anonLoginButton;
    private Button logoutButton;

    Button pwdBtn = new Button("PWD");
    Button lsBtn = new Button("LS");
    Button cdBtn = new Button("CD...");
    Button mkdirBtn = new Button("MKDIR...");
    Button putBtn = new Button("Upload (PUT)...");
    Button getBtn = new Button("Download (GET)...");

    private HBox commandMenu;

    @Override
    public void start(Stage stage) {
        // Row 1: Host
        hostField = new TextField("127.0.0.1");
        hostField.setPromptText("Host");
        hostField.setPrefColumnCount(20);
        HBox row1 = new HBox(5, hostField);
        row1.setPrefHeight(30);

        // Row 2: User
        userField = new TextField("admin");
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

        VBox leftBox = new VBox(5, row1, row2, row3);
        leftBox.setPrefWidth(300);

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

        HBox topPanel = new HBox(10, leftBox, connectButton, loginButton, anonLoginButton, logoutButton);
        topPanel.setPrefHeight(120);

        clientLog = new TextArea();
        clientLog.setEditable(false);
        clientLog.setPromptText("Client commands / actions");

        serverLog = new TextArea();
        serverLog.setEditable(false);
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

        ftpc = new FTPCommands();

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
                cleanupConnection();
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
                cleanupConnection();
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
                        cleanupConnection();
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
                    cleanupConnection();
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
                        cleanupConnection();
                    }
                }
            });
        });
    }

    private void onConnectButtonClicked() {
        if (ftpc == null) {
            ftpc = new FTPCommands();
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
            String greet = ftpc.connect(host, 21);
            clientLog.appendText("Connected to " + host + ".\n");
            if (greet != null) {
                serverLog.appendText(greet + "\n");
            }

            connectButton.setDisable(true);
            loginButton.setDisable(false);
            anonLoginButton.setDisable(false);
            logoutButton.setDisable(false);
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
            String reply = ftpc.login(user, pass);
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
        String anonPass = "password";

        try {
            String reply = ftpc.login(anonUser, anonPass);
            serverLog.appendText(reply + "\n");

            int code = Integer.parseInt(reply.substring(0, 3));
            if (code == 230) {
                clientLog.appendText("Anonymous login successful.\n");
                enableCommandsUI(true);
                loginButton.setDisable(true);
                anonLoginButton.setDisable(true);
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
                ftpc.close();
            }
        } catch (Exception ignore) {}

        ftpc = null;

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