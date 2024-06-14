import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.net.*;
import java.nio.file.*;
import java.text.*;
import java.util.*;

public class HttpServer {
    private static Logger logger = new Logger();
    private static ConfigReader configReader = new ConfigReader();
    private static RequestHandler requestHandler = new RequestHandler();

    public static void main(String[] args) {
        String configFilePath = "src/config.xml";
        if (args.length > 0) {
            configFilePath = args[0];
        }

        configReader.parseConfigFile(configFilePath);

        logger.createLogFileIfNotExists(configReader.getAccessLogPath());
        logger.createLogFileIfNotExists(configReader.getErrorLogPath());

        try (ServerSocket serverSocket = new ServerSocket(configReader.getPort())) {
            System.out.println("Serveur lancé sur le port " + configReader.getPort());

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String clientIP = requestHandler.getIPv4Address(clientSocket.getInetAddress());
                    System.out.println("Connexion depuis l'adresse IP : " + clientIP);

                    if (requestHandler.isRejected(clientIP)) {
                        logger.logError("Connexion refusée pour l'adresse IP : " + clientIP);
                        clientSocket.close();
                        continue;
                    }

                    if (requestHandler.isAccepted(clientIP)) {
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                            String requestLine = in.readLine();
                            if (requestLine != null) {
                                if (requestLine.startsWith("GET /status HTTP/1.1")) {
                                    handleStatusRequest(clientSocket.getOutputStream());
                                } else {
                                    requestHandler.handleClientRequest(clientSocket, clientIP, requestLine);
                                }
                            }
                        }
                    } else {
                        logger.logError("Connexion refusée pour l'adresse IP non acceptée : " + clientIP);
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    logger.logError("Erreur lors de l'acceptation de la connexion : " + e.getMessage());
                }
            }

        } catch (IOException e) {
            logger.logError("Erreur lors du démarrage du serveur : " + e.getMessage());
        }
    }

    private static void handleStatusRequest(OutputStream out) throws IOException {
        // Get memory usage
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long availableMemory = (heapUsage.getMax() - heapUsage.getUsed()) / (1024 * 1024);

        // Get disk space available
        File root = new File("/");
        long freeDiskSpace = root.getFreeSpace() / (1024 * 1024);

        // Get number of running processes
        int numberOfProcesses = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();

        // Construct the HTML response
        String statusMessage = "<html>\n" +
                "<body style=\"background-color: #f0f0f0;\">\n" +
                "<h1>Server Status</h1>\n" +
                "<p>Memoire disponible: " + availableMemory + " MB</p>\n" +
                "<p>Espace disque disponible: " + freeDiskSpace + " MB</p>\n" +
                "<p>Nombre de processeurs: " + numberOfProcesses + "</p>\n" +
                "</body>\n" +
                "</html>";

        String header = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: " + statusMessage.length() + "\r\n" +
                "Content-Type: text/html\r\n" +
                "\r\n";

        out.write(header.getBytes());
        out.write(statusMessage.getBytes());
        out.flush();
    }
}
