package Utility;

import it.polito.appeal.traci.SumoTraciConnection;
import it.polito.appeal.traci.Vehicle;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.Collection;

/**
 * SumoConnector gestisce la connessione a SUMO tramite TraCI4J.
 * Lo scopo è utilizzare SUMO come visualizzatore: gli agenti aggiorneranno
 * la posizione o la velocità dei veicoli, e questa classe invierà i comandi
 * a SUMO.
 */
public class SumoConnector {

    // Riferimento alla connessione TraCI
    private static SumoTraciConnection connection;
    
    /**
     * Stabilisce la connessione con SUMO usando il file di configurazione e un seme.
     *
     * @param configFile percorso del file .sumocfg
     * @param seed       seme casuale per la simulazione
     * @throws Exception se la connessione fallisce
     */
    public static void connect(String configFile, int seed) throws Exception {
        connection = new SumoTraciConnection(configFile, seed);
        connection.runServer(true);  // Avvia la connessione (controlla se questo metodo è bloccante o in thread separato)
        System.out.println("Connesso a SUMO usando il file: " + configFile);
    }
    
    /**
     * Chiude la connessione con SUMO.
     */
    public static void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Connessione SUMO chiusa.");
        }
    }
    
    
    /**
     * Modifica la velocità del veicolo in SUMO.
     *
     * @param vehicleID l'ID del veicolo
     * @param speed     la nuova velocità (in m/s)
     */
    public static synchronized void changeSpeed(String vehicleID, double speed) {
        try {
            Vehicle veh = connection.getVehicleRepository().getByID(vehicleID);
            if (veh != null) {
                veh.changeSpeed(speed);
                System.out.println("SUMO: Velocità di " + vehicleID + " impostata a " + speed + " m/s");
            } else {
                System.err.println("SUMO: Veicolo " + vehicleID + " non trovato.");
            }
        } catch (Exception e) {
            System.err.println("Errore in changeSpeed per " + vehicleID + ": " + e.getMessage());
        }
    }
    
    /**
     * Ottiene la posizione attuale del veicolo da SUMO.
     *
     * @param vehicleID l'ID del veicolo
     * @return una Coordinate con la posizione attuale, oppure (0,0) in caso di errore.
     */
    public static synchronized Coordinate getVehiclePosition(String vehicleID) {
        try {
            Vehicle veh = connection.getVehicleRepository().getByID(vehicleID);
            if (veh != null) {
                Point2D pos = veh.getPosition();
                return new Coordinate((int) pos.getX(), (int) pos.getY());
            } else {
                System.err.println("SUMO: Veicolo " + vehicleID + " non trovato.");
            }
        } catch (Exception e) {
            System.err.println("Errore in getVehiclePosition per " + vehicleID + ": " + e.getMessage());
        }
        return new Coordinate(0, 0);
    }
    
    public static synchronized int getCurrentSimStep() {
        try {
            return connection.getCurrentSimTime();
        } catch (Exception e) {
            System.err.println("Errore in getCurrentSimTime: " + e.getMessage());
        }
        return -1;
    }
    /**
     * Avanza la simulazione di un passo.
     */
    public static synchronized void nextSimStep() {
        try {
            connection.nextSimStep();
            System.out.println("SUMO: Simulazione avanzata di un passo.");
        } catch (Exception e) {
            System.err.println("Errore in nextSimStep: " + e.getMessage());
        }
    }
    
    /**
     * Restituisce il numero totale di veicoli attivi in SUMO.
     *
     * @return il numero di veicoli attivi, oppure -1 in caso di errore.
     */
    public static synchronized int getNumVehicles() {
        try {
            Collection<Vehicle> vehicles = connection.getVehicleRepository().getAll().values();
            return vehicles.size();
        } catch (Exception e) {
            System.err.println("Errore in getNumVehicles: " + e.getMessage());
        }
        return -1;
    }
}
