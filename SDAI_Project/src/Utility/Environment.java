package Utility;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

public class Environment {
    // Lista di incroci veri (junction con più di una corsia in entrata)
    private static List<Intersection> intersections = new ArrayList<>();
    // Lista di nodi (junction con una sola corsia in entrata, tipicamente punti di partenza o arrivo)
    private static List<Intersection> nonIntersectionNodes = new ArrayList<>();
    // Lista di edge della rete
    private static List<Edge> edges = new ArrayList<>();

    /**
     * Carica la mappa leggendo il file XML della rete SUMO.
     * Popola le liste di intersezioni, nodi e edge.
     *
     * @param filename Il percorso del file XML (net.xml).
     */
    public static void loadMap(String filename) {
        try {
            File xmlFile = new File(filename);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // Pulisce le liste precedenti
            intersections.clear();
            nonIntersectionNodes.clear();
            edges.clear();

            // ----- Parsing delle junction (intersezioni e nodi) -----
            NodeList junctionList = doc.getElementsByTagName("junction");
            for (int i = 0; i < junctionList.getLength(); i++) {
                Node node = junctionList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element elem = (Element) node;
                    String type = elem.getAttribute("type");
                    String id = elem.getAttribute("id");
                    // Considera solo junction di tipo "priority" e id non interni (non inizia con ":")
                    if ("priority".equalsIgnoreCase(type) && !id.startsWith(":")) {
                        String xStr = elem.getAttribute("x");
                        String yStr = elem.getAttribute("y");
                        double x = Double.parseDouble(xStr);
                        double y = Double.parseDouble(yStr);
                        String incLanes = elem.getAttribute("incLanes").trim();
                        // Dividi per spazi per contare il numero di corsie in entrata
                        String[] lanes = incLanes.split("\\s+");
                        // Costruisci il nome dell'agente: per esempio "IntersectionAgent_" + id
                        String agentName = "IntersectionAgent_" + id;
                        Intersection junction = new Intersection(id, x, y, agentName);
                        // Se ci sono almeno 2 corsie in entrata, consideralo un vero incrocio
                        if (lanes.length >= 2) {
                            intersections.add(junction);
                        } else {
                            nonIntersectionNodes.add(junction);
                        }
                    }
                }
            }
            System.out.println("Environment: Caricate " + intersections.size() + " intersezioni e " 
                    + nonIntersectionNodes.size() + " nodi non-intersezioni");

            System.out.println("ECoordinate dell'incrocio: " + intersections.get(0).getX() + " " + intersections.get(0).getY()); 

            // ----- Parsing degli edge -----
            NodeList edgeList = doc.getElementsByTagName("edge");
            for (int i = 0; i < edgeList.getLength(); i++) {
                Node node = edgeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element elem = (Element) node;
                    String edgeId = elem.getAttribute("id");
                    // Considera solo gli edge "reali" (id non inizia con ":")
                    if (!edgeId.startsWith(":")) {
                        NodeList laneList = elem.getElementsByTagName("lane");
                        if (laneList.getLength() > 0) {
                            Element laneElem = (Element) laneList.item(0);
                            String shape = laneElem.getAttribute("shape");
                            if (shape != null && !shape.isEmpty()) {
                                String[] coordsStr = shape.trim().split("\\s+");
                                if (coordsStr.length > 0) {
                                    Coordinate start = parseCoordinate(coordsStr[0]);
                                    Coordinate end = parseCoordinate(coordsStr[coordsStr.length - 1]);
                                    double length = Double.parseDouble(laneElem.getAttribute("length"));
                                    Edge edge = new Edge(edgeId, start, end, length);
                                    edges.add(edge);
                                }
                            }
                        }
                    }
                }
            }
            System.out.println("Environment: Caricati " + edges.size() + " edge.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Metodo helper per convertire una stringa "x,y" in un oggetto Coordinate.
     */
    private static Coordinate parseCoordinate(String s) {
        String[] parts = s.split(",");
        double x = Double.parseDouble(parts[0]);
        double y = Double.parseDouble(parts[1]);
        return new Coordinate((int)x, (int)y);
    }
    
    // Metodi accessor per le intersezioni
    public static List<Intersection> getIntersections() {
        return intersections;
    }
    
    public static Intersection getIntersectionByID(String id) {
        for (Intersection inter : intersections) {
            if (inter.getId().equalsIgnoreCase(id)) {
                return inter;
            }
        }
        return null;
    }
    
    // Metodi accessor per i nodi non intersezioni
    public static List<Intersection> getNonIntersectionNodes() {
        return nonIntersectionNodes;
    }
    
    public static Intersection getNonIntersectionNodeByID(String id) {
        for (Intersection node : nonIntersectionNodes) {
            if (node.getId().equalsIgnoreCase(id)) {
                return node;
            }
        }
        return null;
    }
    
    /**
     * Data una posizione e una soglia, restituisce l'intersezione più vicina se la distanza è <= threshold.
     */
    public static Intersection findNearbyIntersection(Coordinate pos, double threshold) {
        Intersection nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Intersection inter : intersections) {
            double dx = inter.getX() - pos.getX();
            double dy = inter.getY() - pos.getY();
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < minDist) {
                minDist = dist;
                nearest = inter;
            }
        }
        if (nearest != null && minDist <= threshold) {
            return nearest;
        }
        return null;
    }
    
    // Metodi accessor per gli edge
    public static List<Edge> getEdges() {
        return edges;
    }
    
    public static Edge getEdgeByID(String id) {
        for (Edge e : edges) {
            if (e.getId().equalsIgnoreCase(id)) {
                return e;
            }
        }
        return null;
    }
}
