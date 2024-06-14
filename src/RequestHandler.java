import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;

/**
 * Classe pour gérer les requêtes des clients.
 */
public class RequestHandler extends HttpServer{

    /**
     * Récupère l'adresse IPv4 à partir de l'adresse InetAddress.
     *
     * @param inetAddress Adresse IP à convertir
     * @return Adresse IPv4 sous forme de chaîne
     */
    public static String getIPv4Address(InetAddress inetAddress) {
        if (inetAddress instanceof Inet4Address) {
            return inetAddress.getHostAddress();
        }
        return "0.0.0.0"; // Adresse par défaut si ce n'est pas une adresse IPv4
    }

    public static boolean isAccepted(String clientIP) {
        for (String acceptedIP : ConfigReader.getAcceptedIPs()) {
            if (clientIP.equals(acceptedIP)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vérifie si l'adresse IP client est dans la liste des adresses IP rejetées.
     *
     * @param clientIP Adresse IP du client
     * @return true si l'adresse IP est rejetée, sinon false
     */
    public static boolean isRejected(String clientIP) {
        for (String rejectedIP : ConfigReader.getRejectedIPs()) {
            if (clientIP.equals(rejectedIP)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gère la requête HTTP du client et envoie une réponse.
     *
     * @param clientSocket Socket client
     * @param clientIP Adresse IP du client
     * @param requestLine Première ligne de la requête HTTP
     */
    public static void handleClientRequest(Socket clientSocket, String clientIP, String requestLine) {
        try {
            String rootDirectory = ConfigReader.getRootDirectory();
            String[] requestParts = requestLine.split(" ");
            String method = requestParts[0];
            String path = requestParts[1];

            System.out.println("Requested path: " + path); // Ajouter ce log pour suivre le chemin demandé

            File file = new File(rootDirectory + path);
            if (file.exists() && !file.isDirectory()) {
                String statusLine = "HTTP/1.1 200 OK\r\n";
                String contentType = getContentType(path);
                String contentLength = "Content-Length: " + file.length() + "\r\n";
                String connection = "Connection: close\r\n\r\n";

                byte[] fileContent = Files.readAllBytes(file.toPath());

                OutputStream outputStream = clientSocket.getOutputStream();
                outputStream.write(statusLine.getBytes());
                outputStream.write(contentType.getBytes());
                outputStream.write(contentLength.getBytes());
                outputStream.write(connection.getBytes());
                outputStream.write(fileContent);
                outputStream.flush();

                Logger.logAccess("Requête traitée avec succès pour l'adresse IP : " + clientIP + ", Ressource : " + path);
            } else {
                String notFoundResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
                clientSocket.getOutputStream().write(notFoundResponse.getBytes());
                Logger.logError("Ressource non trouvée pour l'adresse IP : " + clientIP + ", Ressource : " + path);
            }
        } catch (IOException e) {
            Logger.logError("Erreur lors du traitement de la requête : " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                Logger.logError("Erreur lors de la fermeture du socket client : " + e.getMessage());
            }
        }
    }


    private static String getContentType(String path) {
        if (path.endsWith(".html") || path.endsWith(".htm")) {
            return "Content-Type: text/html\r\n";
        } else if (path.endsWith(".txt") || path.endsWith(".java")) {
            return "Content-Type: text/plain\r\n";
        } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            return "Content-Type: image/jpeg\r\n";
        } else if (path.endsWith(".gif")) {
            return "Content-Type: image/gif\r\n";
        } else if (path.endsWith(".png")) {
            return "Content-Type: image/png\r\n";
        } else if (path.endsWith(".pdf")) {
            return "Content-Type: application/pdf\r\n";
        } else {
            return "Content-Type: application/octet-stream\r\n";
        }
    }
}
