package Utility;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

public class VehicleHelper {
    // Questa classe è responsabile del caricamento e della gestione dei veicoli in un file XML.
    // Contiene una definizione di veicolo e un metodo per caricare i veicoli da un file XML.

    // Definizione di un veicolo


    public static class VehicleDefinition {
        private String id;
        private String type;
        private String route;
        private String depart;
        private String departLane;
        private String departSpeed;

        public VehicleDefinition(String id, String type, String route, String depart, String departLane, String departSpeed) {
            this.id = id;
            this.type = type;
            this.route = route;
            this.depart = depart;
            this.departLane = departLane;
            this.departSpeed = departSpeed;
        }

        public String getId() {
            return id;
        }
        public String getType() {
            return type;
        }
        public String getRoute() {
            return route;
        }
        public String getDepart() {
            return depart;
        }
        public String getDepartLane() {
            return departLane;
        }
        public String getDepartSpeed() {
            return departSpeed;
        }
    }

    public static List<VehicleDefinition> loadVehicles(String filename) {
        List<VehicleDefinition> vehicles = new ArrayList<>();
        try {
            File xmlFile = new File(filename);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList vehicleNodes = doc.getElementsByTagName("vehicle");
            for (int i = 0; i < vehicleNodes.getLength(); i++) {
                Node node = vehicleNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element elem = (Element) node;
                    String id = elem.getAttribute("id");
                    String type = elem.getAttribute("type");
                    String route = elem.getAttribute("route");
                    String depart = elem.getAttribute("depart");
                    String departLane = elem.getAttribute("departLane");
                    String departSpeed = elem.getAttribute("departSpeed");
                    VehicleDefinition vDef = new VehicleDefinition(id, type, route, depart, departLane, departSpeed);
                    vehicles.add(vDef);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vehicles;
    }
}
