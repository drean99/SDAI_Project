package Utility;

import java.util.ArrayList;
import java.util.List;

public class Environment {
    // Lista statica di incroci
    private static List<Intersection> intersections = new ArrayList<>();
    
    /**
     * Inizializza manualmente (hard-coded) la lista di incroci.
     * In un caso reale, potresti caricarli dal file .net.xml.
     */
    static {
        // Esempio: incrocio "C2" alle coordinate (200,200) con l'agente "IntersectionAgent_C2"
        intersections.add(new Intersection("C2", 200.0, 200.0, "IntersectionAgent_C2"));
        
        // Se avessi altri incroci veri, li aggiungeresti qui
        // intersections.add(new Intersection("C5", 300.0, 250.0, "IntersectionAgent_C5"));
    }
    
    /**
     * Restituisce la lista di tutti gli incroci.
     */
    public static List<Intersection> getIntersections() {
        return intersections;
    }
    
    /**
     * Data la posizione del veicolo e una soglia, restituisce l'incrocio più vicino
     * se la distanza è <= threshold, altrimenti null.
     */
    public static Intersection findNearbyIntersection(Coordinate pos, double threshold) {
        Intersection nearest = null;
        double minDist = Double.MAX_VALUE;
        
        for (Intersection inter : intersections) {
            double dx = inter.getX() - pos.getX();
            double dy = inter.getY() - pos.getY();
            double dist = Math.sqrt(dx*dx + dy*dy);
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
}
