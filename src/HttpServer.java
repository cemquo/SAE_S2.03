import java.io.*;
import java.net.*;
import java.nio.file.Files;

public class HttpServer {
    public static void main(String[] args) {
        int port = 80; // Port par défaut
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serveur lancé sur le port " + port);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    handleClientRequest(clientSocket);
                } catch (IOException e) {
                    System.err.println("Erreur lors de l'acceptation de la connexion : " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du démarrage du serveur : " + e.getMessage());
        }
    }

    private static void handleClientRequest(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {

            String requestLine = in.readLine();
            System.out.println("Request: " + requestLine);

            if (requestLine != null && requestLine.startsWith("GET")) {
                String[] tokens = requestLine.split(" ");
                String fileName = tokens[1].substring(1); // Retirer le '/' initial

                if (fileName.isEmpty()) {
                    fileName = "index.html"; // Si aucun fichier n'est spécifié, rediriger vers index.html
                }

                File file = new File(fileName);
                if (file.exists() && !file.isDirectory()) {
                    sendResponse(out, 200, "OK", file);
                } else {
                    sendNotFoundResponse(out);
                }
            }

        } catch (IOException e) {
            System.err.println("Erreur lors du traitement de la requête client : " + e.getMessage());
        }
    }

    private static void sendResponse(OutputStream out, int statusCode, String statusMessage, File file) throws IOException {
        byte[] fileContent = Files.readAllBytes(file.toPath());
        String contentType = Files.probeContentType(file.toPath());
        String header = "HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n" +
                "Content-Length: " + fileContent.length + "\r\n" +
                "Content-Type: " + (contentType != null ? contentType : "application/octet-stream") + "\r\n" +
                "\r\n";
        out.write(header.getBytes());
        out.write(fileContent);
        out.flush();
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
}
