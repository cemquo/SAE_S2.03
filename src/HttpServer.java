import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.io.File;
import java.io.OutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Base64;

public class HttpServer {
    private static int port; // Port par défaut
    private static String rootDirectory; // Répertoire racine par défaut
    private static List<String> acceptedIPs = new ArrayList<>();
    private static List<String> rejectedIPs = new ArrayList<>();
    private static String accessLogPath; // Chemin par défaut du fichier de journal des accès
    private static String errorLogPath; // Chemin par défaut du fichier de journal des erreurs

    public static void main(String[] args) {
        String configFilePath = "src/config.xml"; // Chemin par défaut du fichier de configuration
        if (args.length > 0) {
            configFilePath = args[0];
        }

        // Lire le fichier de configuration
        parseConfigFile(configFilePath);

        // Vérifier et créer les fichiers de log si nécessaire
        createLogFileIfNotExists(accessLogPath);
        createLogFileIfNotExists(errorLogPath);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serveur lancé sur le port " + port);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String clientIP = getIPv4Address(clientSocket.getInetAddress());
                    System.out.println("Connexion depuis l'adresse IP : " + clientIP);

                    if (isRejected(clientIP)) {
                        logError("Connexion refusée pour l'adresse IP : " + clientIP);
                        clientSocket.close();
                        continue;
                    }

                    if (isAccepted(clientIP)) {
                        handleClientRequest(clientSocket, clientIP);
                    } else {
                        logError("Connexion refusée pour l'adresse IP non acceptée : " + clientIP);
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    logError("Erreur lors de l'acceptation de la connexion : " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logError("Erreur lors du démarrage du serveur : " + e.getMessage());
        }
    }

    private static String getIPv4Address(InetAddress inetAddress) {
        if (inetAddress instanceof Inet4Address) {
            return inetAddress.getHostAddress();
        }
        return "0.0.0.0"; // Adresse par défaut si ce n'est pas une adresse IPv4
    }

    private static void parseConfigFile(String configFilePath) {
        try {
            File configFile = new File(configFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(configFile);
            doc.getDocumentElement().normalize();

            NodeList portNode = doc.getElementsByTagName("port");
            if (portNode.getLength() > 0) {
                port = Integer.parseInt(portNode.item(0).getTextContent());
            }

            NodeList rootNode = doc.getElementsByTagName("root");
            if (rootNode.getLength() > 0) {
                rootDirectory = rootNode.item(0).getTextContent();
            }

            NodeList acceptNodes = doc.getElementsByTagName("accept");
            for (int i = 0; i < acceptNodes.getLength(); i++) {
                String ip = acceptNodes.item(i).getTextContent().trim();
                if (!ip.isEmpty()) {
                    acceptedIPs.add(ip);
                }
            }

            NodeList rejectNodes = doc.getElementsByTagName("reject");
            for (int i = 0; i < rejectNodes.getLength(); i++) {
                String ip = rejectNodes.item(i).getTextContent().trim();
                if (!ip.isEmpty()) {
                    rejectedIPs.add(ip);
                }
            }

            NodeList accessLogNode = doc.getElementsByTagName("acceslog");
            if (accessLogNode.getLength() > 0) {
                accessLogPath = accessLogNode.item(0).getTextContent();
            }

            NodeList errorLogNode = doc.getElementsByTagName("errorlog");
            if (errorLogNode.getLength() > 0) {
                errorLogPath = errorLogNode.item(0).getTextContent();
            }

        } catch (Exception e) {
            logError("Erreur lors de la lecture du fichier de configuration : " + e.getMessage());
        }
    }

    private static boolean isAccepted(String clientIP) {
        if (acceptedIPs.isEmpty()) {
            return true; // Accepter toutes les IP si aucune n'est spécifiée
        }
        for (String acceptedIP : acceptedIPs) {
            if (clientIP.matches(acceptedIP.replace(".", "\\.").replace("*", ".*"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRejected(String clientIP) {
        for (String rejectedIP : rejectedIPs) {
            if (clientIP.matches(rejectedIP.replace(".", "\\.").replace("*", ".*"))) {
                return true;
            }
        }
        return false;
    }

    private static void handleClientRequest(Socket clientSocket, String clientIP) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {

            String requestLine = in.readLine();
            System.out.println("Request: " + requestLine);
            logAccess(clientIP + " - " + requestLine);

            if (requestLine != null && requestLine.startsWith("GET")) {
                String[] tokens = requestLine.split(" ");
                String fileName = tokens[1].substring(1); // Retirer le '/' initial

                if (fileName.isEmpty()) {
                    fileName = "index.html"; // Rediriger vers index.html si aucun fichier spécifié
                }

                if (fileName.isEmpty()) {
                    fileName = "index.html"; // Rediriger vers index.html si aucun fichier spécifié
                }

                File file = new File(rootDirectory, fileName);
                if (file.exists() && !file.isDirectory()) {
                    sendResponse(out, 200, "OK", file);
                } else {
                    sendNotFoundResponse(out);
                }
            }

        } catch (IOException e) {
            logError("Erreur lors du traitement de la requête client : " + e.getMessage());
        }
    }

    private static void sendResponse(OutputStream out, int statusCode, String statusMessage, File file) throws IOException {
        byte[] fileContent = Files.readAllBytes(file.toPath());
        String contentType = getContentType(file.getName());

        String header = "HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n" +
                "Content-Length: " + fileContent.length + "\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "\r\n";

        out.write(header.getBytes());
        out.write(fileContent);
        out.flush();
    }

    private static String getContentType(String fileName) {
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".mp3")) {
            return "audio/mpeg";
        } else if (fileName.endsWith(".mp4")) {
            return "video/mp4";
        } else {
            return "application/octet-stream"; // Type par défaut si non déterminé
        }
    }



    private static void sendNotFoundResponse(OutputStream out) throws IOException {
        String notFoundMessage = "<html><body><h1>404 Not Found</h1></body></html>";
        String header = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Length: " + notFoundMessage.length() + "\r\n" +
                "Content-Type: text/html\r\n" +
                "\r\n";
        out.write(header.getBytes());
        out.write(notFoundMessage.getBytes());
        out.flush();
    }

    private static void logAccess(String message) {
        logMessage(message, accessLogPath);
    }

    private static void logError(String message) {
        logMessage(message, errorLogPath);
    }

    private static void logMessage(String message, String logFilePath) {
        try {
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String logEntry = timeStamp + " - " + message + "\n";
            Files.write(new File(logFilePath).toPath(), logEntry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture dans le fichier de journal : " + e.getMessage());
        }
    }

    private static void createLogFileIfNotExists(String logFilePath) {
        try {
            File logFile = new File(logFilePath);
            if (!logFile.exists()) {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du fichier de journal : " + e.getMessage());
        }
    }
}
