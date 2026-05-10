package herrcult69.compnet;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToolBar;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;

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

    private ToolBar commandMenu;
    private BorderPane root;

    @Override
    public void start(Stage stage) {
        initGUI();
        setupEventHandlers(stage);

        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setTitle("FTP Client (JavaFX)");
        stage.setScene(scene);
        stage.show();

        ftpc = new FTPCommands();
        updateUIState(AppState.DISCONNECTED);
    }

    private void initGUI() {
        // Connection Fields

        Label hostLabel = new Label("Host");
        hostLabel.setMinWidth(70);

        hostField = new TextField("127.0.0.1");
        hostField.setPromptText("FTP Server Address");

        HBox row1 = new HBox(8, hostLabel, hostField);
        row1.setAlignment(Pos.CENTER_LEFT);

        Label userLabel = new Label("User");
        userLabel.setMinWidth(70);

        userField = new TextField("admin");
        userField.setPromptText("Username");

        HBox row2 = new HBox(8, userLabel, userField);
        row2.setAlignment(Pos.CENTER_LEFT);

        Label passLabel = new Label("Password");
        passLabel.setMinWidth(70);

        passField = new PasswordField();
        passField.setPromptText("Password");

        HBox row3 = new HBox(8, passLabel, passField);
        row3.setAlignment(Pos.CENTER_LEFT);

        VBox leftBox = new VBox(10, row1, row2, row3);
        leftBox.setPrefWidth(320);
        // Buttons

        connectButton = new Button("Connect");
        connectButton.setPrefSize(120, 40);
        connectButton.getStyleClass().add("primary-button");

        loginButton = new Button("Login");
        loginButton.setPrefSize(120, 40);

        anonLoginButton = new Button("Anonymous");
        anonLoginButton.setPrefSize(140, 40);

        logoutButton = new Button("Logout");
        logoutButton.setPrefSize(120, 40);

        HBox buttonBar = new HBox(
                10,
                connectButton,
                loginButton,
                anonLoginButton,
                logoutButton);

        buttonBar.setAlignment(Pos.CENTER_LEFT);

        VBox topPanel = new VBox(
                15,
                leftBox,
                buttonBar);

        topPanel.setPadding(new Insets(15));
        // Logs

        clientLog = new TextArea();
        clientLog.setEditable(false);
        clientLog.setPromptText("Client commands and actions");

        serverLog = new TextArea();
        serverLog.setEditable(false);
        serverLog.setPromptText("Server replies and file listings");

        Label clientLabel = new Label("Client");
        clientLabel.getStyleClass().add("section-title");

        Label serverLabel = new Label("Server");
        serverLabel.getStyleClass().add("section-title");

        VBox leftLogBox = new VBox(5, clientLabel, clientLog);
        VBox rightLogBox = new VBox(5, serverLabel, serverLog);
        VBox.setVgrow(clientLog, Priority.ALWAYS);
        VBox.setVgrow(serverLog, Priority.ALWAYS);

        SplitPane centerPane = new SplitPane(leftLogBox, rightLogBox);
        centerPane.setDividerPositions(0.5);
        // Bottom Commands

        commandMenu = new ToolBar(
                pwdBtn,
                lsBtn,
                cdBtn,
                putBtn,
                getBtn);
        // Root Layout

        root = new BorderPane();

        root.setPadding(new Insets(10));

        root.setTop(topPanel);
        root.setCenter(centerPane);
        root.setBottom(commandMenu);
    }

    private void setupEventHandlers(Stage stage) {
        connectButton.setOnAction(e -> onConnectButtonClicked());
        loginButton.setOnAction(e -> onLoginButtonClicked());
        anonLoginButton.setOnAction(e -> onAnonymousLoginClicked());
        logoutButton.setOnAction(e -> onLogoutButtonClicked());

        pwdBtn.setOnAction(e -> {
            clientLog.appendText("> pwd\n");
            try {
                ResponseData res = ftpc.sendPWD();
                if (res.isSuccess()) {
                    serverLog.appendText(res.getMessage() + "\n");
                } else {
                    clientLog.appendText("PWD Error: " + res.getMessage() + "\n");
                    updateUIState(AppState.DISCONNECTED);
                }
            } catch (IOException ex) {
                clientLog.appendText("PWD Error: " + ex.getMessage() + "\n");
                updateUIState(AppState.DISCONNECTED);
            }
        });

        lsBtn.setOnAction(e -> {
            clientLog.appendText("> ls\n");
            try {
                ResponseData res = ftpc.PASV_LIST_GUI();
                if (res.isSuccess()) {
                    serverLog.appendText(res.getData() + "\n");
                    serverLog.appendText(res.getMessage() + "\n");
                } else {
                    clientLog.appendText("LS Error: " + res.getMessage() + "\n");
                    updateUIState(AppState.DISCONNECTED);
                }
            } catch (IOException ex) {
                clientLog.appendText("LS Error: " + ex.getMessage() + "\n");
                updateUIState(AppState.DISCONNECTED);
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
                        ResponseData res = ftpc.sendCWD(path);
                        if (res.isSuccess()) {
                            serverLog.appendText(res.getMessage() + "\n");
                        } else {
                            clientLog.appendText("CWD Error: " + res.getMessage() + "\n");
                        }
                    } catch (IOException ex) {
                        clientLog.appendText("CWD Error: " + ex.getMessage() + "\n");
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
                    ResponseData res = ftpc.PASV_STOR(absPath, name);
                    if (res.isSuccess()) {
                        serverLog.appendText("Upload completed for " + name + "\n");
                    } else {
                        clientLog.appendText("Upload Error: " + res.getMessage() + "\n");
                    }
                } catch (IOException ex) {
                    clientLog.appendText("Upload Error: " + ex.getMessage() + "\n");
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
                        ResponseData res = ftpc.PASV_RETR(fileName);
                        if (res.isSuccess()) {
                            serverLog.appendText("Download completed for " + fileName + "\n");
                        } else {
                            clientLog.appendText("Download Error: " + res.getMessage() + "\n");
                        }
                    } catch (IOException ex) {
                        clientLog.appendText("Download Error: " + ex.getMessage() + "\n");
                    }
                }
            });
        });
    }

    private void updateUIState(AppState state) {
        switch (state) {
            case DISCONNECTED:
                connectButton.setDisable(false);
                hostField.setDisable(false);

                loginButton.setDisable(true);
                anonLoginButton.setDisable(true);
                userField.setDisable(true);
                passField.setDisable(true);

                logoutButton.setDisable(true);
                commandMenu.setDisable(true);
                break;

            case CONNECTED:
                connectButton.setDisable(true);
                hostField.setDisable(true);

                loginButton.setDisable(false);
                anonLoginButton.setDisable(false);
                userField.setDisable(false);
                passField.setDisable(false);

                logoutButton.setDisable(false);
                commandMenu.setDisable(true);
                break;

            case LOGGED_IN:
                connectButton.setDisable(true);
                hostField.setDisable(true);

                loginButton.setDisable(true);
                anonLoginButton.setDisable(true);
                userField.setDisable(true);
                passField.setDisable(true);

                logoutButton.setDisable(false);
                commandMenu.setDisable(false);
                break;
        }
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
            ResponseData res = ftpc.connect(host, 21);
            if (res.isSuccess()) {
                clientLog.appendText("Connected to " + host + ".\n");
                serverLog.appendText(res.getMessage() + "\n");
                updateUIState(AppState.CONNECTED);
            } else {
                clientLog.appendText("Connection failed: " + res.getMessage() + "\n");
                updateUIState(AppState.DISCONNECTED);
            }
        } catch (IOException ex) {
            clientLog.appendText("Connection error: " + ex.getMessage() + "\n");
            updateUIState(AppState.DISCONNECTED);
        }
    }

    private void onLoginButtonClicked() {
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
            ResponseData res = ftpc.login(user, pass);
            if (res.isSuccess()) {
                clientLog.appendText("Login successful as " + user + ".\n");
                serverLog.appendText(res.getMessage() + "\n");
                updateUIState(AppState.LOGGED_IN);
            } else {
                clientLog.appendText("Login failed: " + res.getMessage() + "\n");
            }
        } catch (IOException ex) {
            clientLog.appendText("Login error: " + ex.getMessage() + "\n");
        }
    }

    private void onAnonymousLoginClicked() {
        try {
            ResponseData res = ftpc.loginAnonymous();
            if (res.isSuccess()) {
                clientLog.appendText("Anonymous login successful.\n");
                serverLog.appendText(res.getMessage() + "\n");
                updateUIState(AppState.LOGGED_IN);
            } else {
                clientLog.appendText("Anonymous login failed: " + res.getMessage() + "\n");
            }
        } catch (IOException ex) {
            clientLog.appendText("Anonymous login error: " + ex.getMessage() + "\n");
        }
    }

    private void onLogoutButtonClicked() {
        clientLog.appendText("Disconnecting...\n");
        if (ftpc != null) {
            ftpc.close();
        }
        updateUIState(AppState.DISCONNECTED);
        ftpc = new FTPCommands(); // Reset state
    }

    public static void main(String[] args) {
        launch(args);
    }
}