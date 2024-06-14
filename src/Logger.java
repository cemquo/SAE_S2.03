import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Classe pour gérer l'enregistrement de logs.
 */
public class Logger extends HttpServer{

    /**
     * Crée le fichier de log spécifié s'il n'existe pas déjà.
     *
     * @param logFilePath Chemin du fichier de log
     */
    public static void createLogFileIfNotExists(String logFilePath) {
        try {
            if (!Files.exists(Paths.get(logFilePath))) {
                Files.createDirectories(Paths.get(logFilePath).getParent());
                Files.createFile(Paths.get(logFilePath));
                System.out.println("Fichier de log créé : " + logFilePath);
            } else {
                System.out.println("Le fichier de log existe déjà : " + logFilePath);
            }
        } catch (IOException e) {
            // Utilisation de e.printStackTrace() pour obtenir une trace complète de l'erreur
            e.printStackTrace();
        }
    }

    /**
     * Enregistre un message d'erreur dans le fichier de log.
     *
     * @param errorMessage Message d'erreur à enregistrer
     */
    public static void logError(String errorMessage) {
        logMessage(errorMessage, ConfigReader.getErrorLogPath());
    }

    /**
     * Enregistre un message d'accès dans le fichier de log.
     *
     * @param accessMessage Message d'accès à enregistrer
     */
    public static void logAccess(String accessMessage) {
        logMessage(accessMessage, ConfigReader.getAccessLogPath());
    }

    private static void logMessage(String message, String logFilePath) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String formattedMessage = "[" + timeStamp + "] " + message + "\n";
        try {
            Files.write(Paths.get(logFilePath), formattedMessage.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture dans le fichier de log : " + e.getMessage());
        }
    }
}
