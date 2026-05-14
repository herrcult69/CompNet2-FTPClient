package herrcult69.compnet;

import javafx.application.Application;
import javafx.application.Platform;
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
    Button rmdBtn = new Button("RMDIR...");
    Button putBtn = new Button("Upload (PUT)...");
    Button getBtn = new Button("Download (GET)...");

    private ToolBar commandMenu;
    private BorderPane root; // A 5 region layout t-b-l-r-c

    // This method is called by JavaFX where main call launch(args) similar to the
    // void run() in Threads.
    // BUild the UI, create the Event handlers, get the style from CSS file
    // Show the App Window on the screen and init the FTP Controller
    @Override
    public void start(Stage stage) {
        initGUI();
        setupEventHandlers(stage);

        // The stage is the window and the scene is the content on the screen built to
        // root
        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm()); // Ad the css rules to the scene's rule book (set of style each element can use)
        stage.setTitle("FTP Client (JavaFX)");
        stage.setScene(scene); // place scene on window then shows the window
        stage.show();

        try {
            ftpc = new FTPCommands();
            updateUIState(AppState.DISCONNECTED);
        } catch (IOException e) {
            clientLog.appendText("** CLIENT ERROR - NO CONFIG FILE FOUND **\n");
            clientLog.appendText("** PLEASE EXIT THE PROGRAM **\n");
            updateUIState(AppState.DISCONNECTED);
            connectButton.setDisable(true);
        }

    }

    private void initGUI() {
        // Creating connection Fields
        Label hostLabel = new Label("Host");
        hostLabel.setMinWidth(70);

        hostField = new TextField("localhost");
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
        // Creating connection Buttons

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

        topPanel.setPadding(new Insets(15)); // add Padding surrounding the topbar (Inset = empty space)
        // Logs

        clientLog = new TextArea();
        clientLog.setEditable(false); // disable abi9lity to input data in
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
        VBox.setVgrow(clientLog, Priority.ALWAYS); // allow the logs to grow vertically when window grows
        VBox.setVgrow(serverLog, Priority.ALWAYS);

        SplitPane centerPane = new SplitPane(leftLogBox, rightLogBox);// auto share available space btween 2 panes so it
                                                                      // dynamic
        centerPane.setDividerPositions(0.5);
        // Bottom Commands

        commandMenu = new ToolBar(
                pwdBtn,
                lsBtn,
                cdBtn,
                mkdirBtn,
                rmdBtn,
                putBtn,
                getBtn);
        // Root Layout

        root = new BorderPane();

        root.setPadding(new Insets(10));

        root.setTop(topPanel);
        root.setCenter(centerPane);
        root.setBottom(commandMenu);
    }

    // Assign event to functions
    private void setupEventHandlers(Stage stage) {
        connectButton.setOnAction(e -> onConnectButtonClicked());
        loginButton.setOnAction(e -> onLoginButtonClicked());
        anonLoginButton.setOnAction(e -> onAnonymousLoginClicked());
        logoutButton.setOnAction(e -> onLogoutButtonClicked());

        pwdBtn.setOnAction(e -> onPwdButtonClicked());
        lsBtn.setOnAction(e -> onLsButtonClicked());
        cdBtn.setOnAction(e -> onCdButtonClicked());
        mkdirBtn.setOnAction(e -> onMkdirButtonClicked());
        rmdBtn.setOnAction(e -> onRmdButtonClicked());
        putBtn.setOnAction(e -> onPutButtonClicked(stage));
        getBtn.setOnAction(e -> onGetButtonClicked());
    }

    private void onConnectButtonClicked() { // Read the hostField then open a new thread to make the connection so the gui dont freeze
                                            // NOTE THAT: Platform runlater was used because in JAVA fx only 1 thread can run the talk to the UI. 
                                            // Thread was use because connection to Server do take a long time
        String host = hostField.getText().trim();
        if (host.isEmpty()) {
            clientLog.appendText("Host is required.\n");
            hostField.getStyleClass().add("error-field");
            return;
        } else {
            hostField.getStyleClass().remove("error-field");
        }
        clientLog.appendText("> Connecting to " + host + "...\n");
        connectButton.setDisable(true);

        new Thread(() -> {
            try {

                ResponseData result = ftpc.connect(host, 21);
                // JUMP BACK TO UI THREAD WITH THE RESULT
                Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        clientLog.appendText("Connected to " + host + ".\n");
                        serverLog.appendText(result.getMessage() + "\n");
                        updateUIState(AppState.CONNECTED);
                    } else {
                        clientLog.appendText("Connection failed: " + result.getMessage() + "\n");
                        updateUIState(AppState.DISCONNECTED);
                    }
                });

            } catch (IOException ex) {
                // JUMP BACK TO UI THREAD FOR ERRORS
                Platform.runLater(() -> {
                    clientLog.appendText("Connection error: " + ex.getMessage() + "\n");
                    updateUIState(AppState.DISCONNECTED);
                });
            }
        }).start();
    }

    // Normal Login with required name and password
    private void onLoginButtonClicked() { // Read user and pass from the its field, pass it to ftpc for it to login with the server via socket
        String user = userField.getText().trim();
        String pass = passField.getText();

        if (user.isEmpty()) {
            clientLog.appendText("User is required.\n");
            userField.getStyleClass().add("error-field");
            return;
        } else {
            userField.getStyleClass().remove("error-field");
        }

        try {
            ResponseData result = ftpc.login(user, pass);
            if (result.isSuccess()) {
                clientLog.appendText("Login successful as " + user + ".\n");
                serverLog.appendText(result.getMessage() + "\n");
                updateUIState(AppState.LOGGED_IN);
            } else {
                clientLog.appendText("Login failed: " + result.getMessage() + "\n");
            }
        } catch (IOException ex) {
            clientLog.appendText("Login error: " + ex.getMessage() + "\n");
        }
    }
     
    // Anonymous: Automatically login to server using annonymous credential (only support some server)
    private void onAnonymousLoginClicked() {
        try {
            String host = hostField.getText().trim();
            ResponseData res = ftpc.loginAnonymous(host);
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

    // Logout -> disconnect to server via QUIT command. Return the APP to default state ready to connect to new serer
    private void onLogoutButtonClicked() {
        clientLog.appendText("Disconnecting...\n");
        ftpc.close(); // all data related to socket is cleaned, socket cant be reopen
        updateUIState(AppState.DISCONNECTED);
        try {
            ftpc = new FTPCommands(); // fresh start discarding old object for java trash collector
        } catch (IOException e) {
            clientLog.appendText("** CLIENT ERROR - NO CONFIG FILE FOUND **\n");
            clientLog.appendText("** PLEASE EXIT THE PROGRAM **\n");
            updateUIState(AppState.DISCONNECTED);
            connectButton.setDisable(true);
        }

    }
    // PWD: show name of current dir in.
    private void onPwdButtonClicked() {
        clientLog.appendText("> pwd\n");
        try {
            ResponseData result = ftpc.sendPWD();
            if (result.isSuccess()) {
                serverLog.appendText(result.getMessage() + "\n");
            } else {
                clientLog.appendText("PWD Error: " + result.getMessage() + "\n");
            }
        } catch (IOException ex) {
            clientLog.appendText("PWD Error: " + ex.getMessage() + "\n");
        }
    }

    // Ls: LIST open a thread to get the ls output from server, this command send alot of data -> long wait -> thread
    private void onLsButtonClicked() {
        clientLog.appendText("> ls\n");
        lsBtn.setDisable(true);

        new Thread(() -> {
            try {
                ResponseData res = ftpc.PASV_LIST_GUI();
                Platform.runLater(() -> {
                    if (res.isSuccess()) {
                        serverLog.appendText(res.getData() + "\n");
                        serverLog.appendText(res.getMessage() + "\n");
                    } else {
                        clientLog.appendText("LS Error: " + res.getMessage() + "\n");
                    }
                    lsBtn.setDisable(false);
                });
            } catch (IOException ex) {
                Platform.runLater(() -> {
                    clientLog.appendText("LS Error: " + ex.getMessage() + "\n");
                    lsBtn.setDisable(false);
                });
            }
        }).start();
    }

    // CD change dir, button when press will create a Input Dialog Box, user enter name of dir (the UI will froze whilst dialog is open)
    private void onCdButtonClicked() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Change Directory");
        dialog.setHeaderText("Enter directory path:");
        dialog.setContentText("Path:");
        dialog.showAndWait().ifPresent(path -> { // not Present mean cancel input
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
    }

    // MKDIR create dir
    private void onMkdirButtonClicked() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Directory");
        dialog.setHeaderText("Enter new directory name:");
        dialog.setContentText("Name:");
        dialog.showAndWait().ifPresent(dirName -> {
            if (!dirName.trim().isEmpty()) {
                clientLog.appendText("> mkdir " + dirName + "\n");
                try {
                    ResponseData res = ftpc.sendMKD(dirName);
                    if (res.isSuccess()) {
                        serverLog.appendText(res.getMessage() + "\n");
                    } else {
                        clientLog.appendText("MKDIR Error: " + res.getMessage() + "\n");
                    }
                } catch (IOException ex) {
                    clientLog.appendText("MKDIR Error: " + ex.getMessage() + "\n");
                }
            }
        });
    }

    // RMDIR remove dir
    private void onRmdButtonClicked() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Remove Directory");
        dialog.setHeaderText("Enter directory name to remove:");
        dialog.setContentText("Name:");
        dialog.showAndWait().ifPresent(dirName -> {
            if (!dirName.trim().isEmpty()) {
                clientLog.appendText("> rmdir " + dirName + "\n");
                try {
                    ResponseData res = ftpc.sendRMD(dirName);
                    if (res.isSuccess()) {
                        serverLog.appendText(res.getMessage() + "\n");
                    } else {
                        clientLog.appendText("RMDIR Error: " + res.getMessage() + "\n");
                    }
                } catch (IOException ex) {
                    clientLog.appendText("RMDIR Error: " + ex.getMessage() + "\n");
                }
            }
        });
    }

    // PUT button when pressed will open up a file choose menu where u can choose the file, the path will be return and a thread is used to upload that file.
    private void onPutButtonClicked(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Upload");
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            String absPath = selectedFile.getAbsolutePath();
            String name = selectedFile.getName();
            clientLog.appendText("> put " + absPath + "\n");
            clientLog.appendText("Uploading for " + name + "\n");
            putBtn.setDisable(true);
            new Thread(() -> {
                try {
                    ResponseData res = ftpc.PASV_STOR(absPath, name);
                    Platform.runLater(() -> {
                        if (res.isSuccess()) {
                            serverLog.appendText("Upload completed for " + name + "\n");
                        } else {
                            clientLog.appendText("Upload Error: " + res.getMessage() + "\n");
                        }
                        putBtn.setDisable(false);
                    });
                } catch (IOException ex) {
                    Platform.runLater(() -> {
                        clientLog.appendText("Upload Error: " + ex.getMessage() + "\n");
                        putBtn.setDisable(false);
                    });
                }
            }).start();
        }
    }

    // Get button when press will open a dialog where user type in the filename of the remote file on server, server will send back the downloaded file.
    private void onGetButtonClicked() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Download File");
        dialog.setHeaderText("Enter remote file name to download:");
        dialog.setContentText("File name:");
        dialog.showAndWait().ifPresent(fileName -> { // not Present mean cancel input
            if (!fileName.trim().isEmpty()) {
                clientLog.appendText("> get " + fileName + "\n");
                clientLog.appendText("Downloading for " + fileName + "\n");
                getBtn.setDisable(true);
                new Thread(() -> {
                    try {
                        ResponseData res = ftpc.PASV_RETR(fileName);
                        Platform.runLater(() -> {
                            if (res.isSuccess()) {
                                serverLog.appendText("Download completed for " + fileName + "\n");
                            } else {
                                clientLog.appendText("Download Error: " + res.getMessage() + "\n");
                            }
                            getBtn.setDisable(false);
                        });
                    } catch (IOException ex) {
                        Platform.runLater(() -> {
                            clientLog.appendText("Download Error: " + ex.getMessage() + "\n");
                            getBtn.setDisable(false);
                        });
                    }
                }).start();
            }
        });
    }

    // Function of Convenient, update the UI upon state, DISCONNECTED CONNECTED LOGGED_IN
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

    public static void main(String[] args) {
        launch(args);
    }
}