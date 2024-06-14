import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe pour lire et parser le fichier de configuration XML.
 */
public class ConfigReader {

    private static int port; // Port par défaut
    private static String rootDirectory; // Répertoire racine par défaut
    private static List<String> acceptedIPs = new ArrayList<>();
    private static List<String> rejectedIPs = new ArrayList<>();
    private static String accessLogPath; // Chemin par défaut du fichier de journal des accès
    private static String errorLogPath; // Chemin par défaut du fichier de journal des erreurs

    /**
     * Parse le fichier de configuration XML pour initialiser les paramètres du serveur.
     *
     * @param configFilePath Chemin du fichier de configuration XML
     */
    public static void parseConfigFile(String configFilePath) {
        try {
            File configFile = new File(configFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(configFile);
            doc.getDocumentElement().normalize();

            // Lire les paramètres de configuration
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
            Logger.logError("Erreur lors de la lecture du fichier de configuration : " + e.getMessage());
        }
    }

    public static int getPort() {
        return port;
    }

    public static String getRootDirectory() {
        return rootDirectory;
    }

    public static List<String> getAcceptedIPs() {
        return acceptedIPs;
    }

    public static List<String> getRejectedIPs() {
        return rejectedIPs;
    }

    public static String getAccessLogPath() {
        return accessLogPath;
    }

    public static String getErrorLogPath() {
        return errorLogPath;
    }
}
