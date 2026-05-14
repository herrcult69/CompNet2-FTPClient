# ComputerNetwork2 Project: Client Implementation of TCP Protocol using Java

## Overview
This project is a custom FTP (File Transfer Protocol) Client built from scratch using Java as part of the Computer Network 2 coursework. It demonstrates a working implementation of application-layer protocols over TCP sockets, interacting directly with FTP servers through raw commands and handling passive data socket transfers for file management.

## Prerequisites
To build and run this application, you will need:
- **Java Development Kit (JDK):** Version 11 or higher (with JavaFX support)
- **Maven:** For dependency management and building the project
- An active internet connection or a local network mapped to an FTP server for testing.

## Technology Used (Libraries)
- **Core Java (`java.net`, `java.io`):** Bare-metal TCP `Socket` programming, `InputStream`/`OutputStream` management, and standard I/O buffers for protocol data transmission.
- **JavaFX:** Providing the graphical user interface (GUI), interactive dialogs, and platform-threading (`Platform.runLater`) for non-blocking UI updates.
- **Maven:** Project architecture, lifecycle management, and build automation.

## UI Visuals
*(Replace the placeholder links below with actual screenshots of your application)*

### Login Screen
![Login Screen Placeholder](path/to/login_image.png)
*Description: The login interface allowing user authentication or Anonymous login configurations.*

### Main Dashboard & File Operations
![Main UI Placeholder](path/to/main_dashboard_image.png)
*Description: The central hub where users can see server logs, client command history, and execute various FTP procedures using the command toolbar.*

## How to Run It

### Using Maven (Command Line)
1. Open your terminal and navigate to the project's root directory (where `pom.xml` is located):
   ```bash
   cd /path/to/CompNet2-FTPClient
   ```
2. Clean and compile the project:
   ```bash
   mvn clean compile
   ```
3. Run the application:
   *(Assuming you have the JavaFX maven plugin or exec plugin configured)*
   ```bash
   mvn javafx:run
   # OR
   mvn exec:java -Dexec.mainClass="herrcult69.compnet.AppFTP"
   ```

## Configuration & Resources
This application externalizes UI styling and server configurations using the `src/main/resources/` directory:
- **`style.css`**: Contains all JavaFX styling rules for the UI.
- **`config.properties`**: Manages known server profiles. You can easily edit this file to add or modify anonymous login credentials for specific server IPs. The application reads these mappings seamlessly via the ClassLoader, allowing you to scale login profiles without modifying backend Java code.

## Features
- **Authentication:** Standard User/Password login as well as Anonymous profile logging.
- **Passive Mode (PASV):** Seamless data connection brokering for data integrity across firewalls.
- **Directory Navigation:** 
  - Print Working Directory (`PWD`)
  - Change Directory (`CWD`)
  - List contents (`LIST`)
- **Directory Management:** 
  - Make Directory (`MKD`)
  - Remove Directory (`RMD`)
- **File Transfer (Binary/Image Mode `TYPE I`):** 
  - Upload files to the server (`STOR`)
  - Download files from the server (`RETR`)
  - Delete files from the server (`DELE`)
- **Live Logging:** Real-time dual-console output showcasing both Client Commands and Raw Server Responses for deep protocol visibility.

## Developer
- **Name:** herrcult69 (Huynh Van Hoa Le)
- **GitHub:** [CompNet2-FTPClient](https://github.com/herrcult69/CompNet2-FTPClient)