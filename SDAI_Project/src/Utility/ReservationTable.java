package Utility;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ReservationTable {
    private List<Reservation> reservations;

    public ReservationTable() {
        reservations = new ArrayList<>();
    }
    
    /**
     * Restituisce la corsia a destra di quella passata come parametro.
     * Ad esempio, per "north" restituisce "east", per "east" restituisce "south", ecc.
     */
    private String getRightLane(String lane) {
        if(lane == null) return "";
        switch(lane.toLowerCase()) {
            case "north": return "east";
            case "east": return "south";
            case "south": return "west";
            case "west": return "north";
            default: return "";
        }
    }
    
    /**
     * Verifica se la prenotazione richiesta (richiesta) è compatibile con quelle già presenti,
     * applicando le seguenti logiche:
     * 
     * 1. Priority-based policy: la richiesta con priorità maggiore prevale.
     * 2. With-lane-based policy (FCFS): se le richieste provengono dalla stessa corsia, quella
     *    arrivata prima (con startTime minore) vince.
     * 3. Priority to right: se le richieste hanno la stessa priorità e corsie differenti,
     *    il veicolo che arriva da destra ha la precedenza.
     * 
     * Se esiste un conflitto non risolvibile a favore della nuova richiesta, viene restituito false.
     * In caso contrario, tutte le prenotazioni conflittanti vengono rimosse e viene restituito true.
     */
    public boolean checkAvailability(Reservation richiesta) {
        Iterator<Reservation> it = reservations.iterator();
        while (it.hasNext()) {
            Reservation res = it.next();
            if (res.conflictsWith(richiesta)) {
                // 1. Controllo della priorità
                if (richiesta.getPriority() > res.getPriority()) {
                    // Nuova richiesta ha priorità maggiore: rimuovo la prenotazione conflittante
                    it.remove();
                } else if (richiesta.getPriority() < res.getPriority()) {
                    // Nuova richiesta ha priorità inferiore: rifiuta
                    return false;
                } else {
                    // Stessa priorità: si applica la policy sulla corsia
                    if (richiesta.getArrivalLane().equalsIgnoreCase(res.getArrivalLane())) {
                        // Stessa corsia: FCFS – vince quella con startTime minore
                        if (richiesta.getTimeSlot().getStartTime() < res.getTimeSlot().getStartTime()) {
                            it.remove();
                        } else {
                            return false;
                        }
                    } else {
                        // Corsie differenti: applica la regola della precedenza a destra
                        String rightLaneForNew = getRightLane(richiesta.getArrivalLane());
                        if (res.getArrivalLane().equalsIgnoreCase(rightLaneForNew)) {
                            // Il conflitto proviene dalla destra della nuova richiesta: questa deve cedere
                            return false;
                        } else {
                            String rightLaneForRes = getRightLane(res.getArrivalLane());
                            if (richiesta.getArrivalLane().equalsIgnoreCase(rightLaneForRes)) {
                                // La nuova richiesta è da destra rispetto al conflitto: prevale
                                it.remove();
                            } else {
                                // Se nessuna delle due regole "destra" è determinante, ricorriamo a FCFS
                                if (richiesta.getTimeSlot().getStartTime() < res.getTimeSlot().getStartTime()) {
                                    it.remove();
                                } else {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * Aggiunge una nuova prenotazione alla tabella.
     */
    public void add(Reservation richiesta) {
        reservations.add(richiesta);
    }
    
    // Eventuali metodi aggiuntivi, per la rimozione di prenotazioni scadute, possono essere implementati qui.
}
