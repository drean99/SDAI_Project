import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class IntersectionAgent extends Agent {

    private final long TIMEOUT = 10000; // Timeout in millisecondi per considerare scaduta una richiesta
    // Lista di richieste pendenti
    private List<Request> pendingRequests = new ArrayList<>();
    // Lista delle richieste attualmente autorizzate (massimo 2 contemporaneamente)
    private List<Request> currentRequests = new ArrayList<>();

    // Classe interna per rappresentare una richiesta di passaggio
    private class Request {
        String vehicleID;
        String arrivalLane;
        String turningIntention;
        long timestamp;
        int priority; // 0 = base, valori maggiori indicano una priorità più alta

        public Request(String vehicleID, String arrivalLane, String turningIntention, long timestamp, int priority) {
            this.vehicleID = vehicleID;
            this.arrivalLane = arrivalLane.toLowerCase();
            this.turningIntention = turningIntention.toLowerCase();
            this.timestamp = timestamp;
            this.priority = priority;
        }
        
        @Override
        public String toString() {
            return "[" + vehicleID + ", " + arrivalLane + ", " + turningIntention + ", " + timestamp + ", p=" + priority + "]";
        }
    }

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " avviato come IntersectionAgent.");
        addBehaviour(new ProcessMessagesBehaviour());
        addBehaviour(new QueueProcessingBehaviour(this, 100));
    }

    // Comportamento per ricevere i messaggi (sia REQUEST_PASS che PASSED)
    private class ProcessMessagesBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                String content = msg.getContent().trim();
                // Gestione del messaggio PASSED
                if (content.startsWith("PASSED,")) {
                    String passedVehicle = content.substring("PASSED,".length()).trim();
                    // Se il veicolo autorizzato (fra quelli correnti) ha attraversato, lo rimuoviamo
                    Iterator<Request> it = currentRequests.iterator();
                    boolean removed = false;
                    while (it.hasNext()) {
                        Request req = it.next();
                        if (req.vehicleID.equalsIgnoreCase(passedVehicle)) {
                            it.remove();
                            removed = true;
                            System.out.println("IntersectionAgent: " + passedVehicle + " ha attraversato (rimosso da currentRequests).");
                            break;
                        }
                    }
                    // Inoltre, rimuoviamo eventuali richieste pendenti per quel veicolo
                    it = pendingRequests.iterator();
                    while (it.hasNext()) {
                        Request req = it.next();
                        if (req.vehicleID.equalsIgnoreCase(passedVehicle)) {
                            it.remove();
                            System.out.println("IntersectionAgent: richiesta di " + passedVehicle + " rimossa dalla coda (PASSED ricevuto).");
                        }
                    }
                }
                // Gestione delle richieste di passaggio
                else if (content.startsWith("REQUEST_PASS,")) {
                    String[] parts = content.split(",");
                    if (parts.length < 6) {
                        System.out.println("Formato messaggio non valido: " + content);
                        return;
                    }
                    String vehicleID = parts[1].trim();
                    String arrivalLane = parts[2].trim().toLowerCase();
                    String turningIntention = parts[3].trim().toLowerCase();
                    long timestamp;
                    try {
                        timestamp = Long.parseLong(parts[4].trim());
                    } catch (NumberFormatException e) {
                        timestamp = System.currentTimeMillis();
                    }
                    int priority;
                    try {
                        priority = Integer.parseInt(parts[5].trim());
                    } catch (NumberFormatException e) {
                        priority = 0;
                    }
                    Request newReq = new Request(vehicleID, arrivalLane, turningIntention, timestamp, priority);
                    if (!containsRequest(vehicleID)) {
                        pendingRequests.add(newReq);
                        System.out.println("IntersectionAgent: richiesta aggiunta da " + vehicleID + " -> " + newReq);
                    } else {
                        System.out.println("IntersectionAgent: richiesta già presente per " + vehicleID);
                    }
                } else {
                    System.out.println("IntersectionAgent: formato messaggio non riconosciuto: " + content);
                }
            } else {
                block();
            }
        }
    }

    // Comportamento che elabora periodicamente la coda delle richieste
    private class QueueProcessingBehaviour extends TickerBehaviour {
        public QueueProcessingBehaviour(Agent a, long period) {
            super(a, period);
        }
        @Override
        protected void onTick() {
            long now = System.currentTimeMillis();

            // Rimuovi eventuali currentRequests scadute
            Iterator<Request> it = currentRequests.iterator();
            while (it.hasNext()) {
                Request req = it.next();
                if (now - req.timestamp > TIMEOUT) {
                    System.out.println("IntersectionAgent: Timeout per " + req.vehicleID + " (rimosso da currentRequests).");
                    sendEnd(req.vehicleID);
                    it.remove();
                }
            }

            // Ordina le richieste pendenti: priorità decrescente, se uguale per timestamp crescente
            Collections.sort(pendingRequests, new Comparator<Request>() {
                @Override
                public int compare(Request r1, Request r2) {
                    if (r2.priority != r1.priority) {
                        return Integer.compare(r2.priority, r1.priority);
                    }
                    return Long.compare(r1.timestamp, r2.timestamp);
                }
            });

            // Se non abbiamo alcuna richiesta autorizzata e ci sono richieste pendenti, autorizza la prima
            if (currentRequests.isEmpty() && !pendingRequests.isEmpty()) {
                Request first = pendingRequests.remove(0);
                currentRequests.add(first);
                sendGo(first.vehicleID);
                System.out.println("IntersectionAgent: autorizzato " + first.vehicleID + " (p=" + first.priority + ").");
            }

            // Se abbiamo una sola richiesta autorizzata e ce ne sono altre pendenti,
            // prova ad autorizzare (in aggiunta) una seconda richiesta se compatibile
            if (currentRequests.size() == 1 && !pendingRequests.isEmpty()) {
                for(int i=0; i< Math.min(2, pendingRequests.size()); i++){
                    Request candidate = pendingRequests.get(i);
                    if (canGoTogether(currentRequests.get(0), candidate)) {
                        pendingRequests.remove(i);
                        currentRequests.add(candidate);
                        sendGo(candidate.vehicleID);
                        System.out.println("IntersectionAgent: autorizzato concomitantemente " + candidate.vehicleID +
                                " insieme a " + currentRequests.get(0).vehicleID);
                        break;
                    }
                }
            }

            // Se abbiamo già due richieste autorizzate, non facciamo altro finché non ne scade almeno una.
        }
    }

/**
 * Determina se due veicoli possono procedere contemporaneamente, considerando sia
 * la loro direzione di arrivo (arrivalLane) sia l'intenzione (turningIntention).
 */
private boolean canGoTogether(Request r1, Request r2) {
    // Caso base: entrambi vanno dritti
    if (r1.turningIntention.equals("straight") && r2.turningIntention.equals("straight")) {
        if ((r1.arrivalLane.equals("east") && r2.arrivalLane.equals("west")) ||
            (r1.arrivalLane.equals("west") && r2.arrivalLane.equals("east")) ||
            (r1.arrivalLane.equals("north") && r2.arrivalLane.equals("south")) ||
            (r1.arrivalLane.equals("south") && r2.arrivalLane.equals("north"))) {
            return true;
        }
    }
    
    // Logica per la svolta a destra
    if (r1.turningIntention.equals("right")) {
        // Se r2 proviene dalla direzione opposta, r2 deve andare dritto o svolta a destra
        if (r2.arrivalLane.equals(getOpposite(r1.arrivalLane))) {
            if (r2.turningIntention.equals("straight") || r2.turningIntention.equals("right"))
                return true;
        }
        // Se r2 proviene dalla corsia precedente, r2 può svoltare sia a destra che a sinistra
        if (r2.arrivalLane.equals(getPrevious(r1.arrivalLane))) {
            if (r2.turningIntention.equals("right") || r2.turningIntention.equals("left"))
                return true;
        }
        // Se r2 proviene dalla corsia successiva, non ci sono conflitti
        if (r2.arrivalLane.equals(getNext(r1.arrivalLane))) {
            return true;
        }
    }
    
    if (r2.turningIntention.equals("right")) {
        if (r1.arrivalLane.equals(getOpposite(r2.arrivalLane))) {
            if (r1.turningIntention.equals("straight") || r1.turningIntention.equals("right"))
                return true;
        }
        if (r1.arrivalLane.equals(getPrevious(r2.arrivalLane))) {
            if (r1.turningIntention.equals("right") || r1.turningIntention.equals("left"))
                return true;
        }
        if (r1.arrivalLane.equals(getNext(r2.arrivalLane))) {
            return true;
        }
    }
    
    // Logica per la svolta a sinistra
    if (r1.turningIntention.equals("left")) {
        // Possono procedere insieme se:
        // - r2 proviene dalla direzione opposta e sta svoltando a sinistra
        if (r2.arrivalLane.equals(getOpposite(r1.arrivalLane)) && r2.turningIntention.equals("left"))
            return true;
        // - oppure se r2 proviene dalla corsia successiva e sta svoltando a destra
        if (r2.arrivalLane.equals(getNext(r1.arrivalLane)) && r2.turningIntention.equals("right"))
            return true;
    }
    
    if (r2.turningIntention.equals("left")) {
        if (r1.arrivalLane.equals(getOpposite(r2.arrivalLane)) && r1.turningIntention.equals("left"))
            return true;
        if (r1.arrivalLane.equals(getNext(r2.arrivalLane)) && r1.turningIntention.equals("right"))
            return true;
    }
    
    return false;
}

/**
 * Restituisce la direzione opposta a quella data.
 * Esempio: opposite di "north" è "south", di "east" è "west", ecc.
 */
private String getOpposite(String direction) {
    switch(direction.toLowerCase()){
        case "north": return "south";
        case "south": return "north";
        case "east":  return "west";
        case "west":  return "east";
        default:      return "";
    }
}

/**
 * Restituisce la direzione precedente rispetto a quella data, considerando l'ordine:
 * north -> west -> south -> east.  
 * Ad esempio, la precedente di "north" è "west", quella di "east" è "north", ecc.
 */
private String getPrevious(String direction) {
    switch(direction.toLowerCase()){
        case "north": return "east";
        case "east":  return "south";
        case "south": return "west";
        case "west":  return "north";
        default:      return "";
    }
}

/**
 * Restituisce la direzione successiva rispetto a quella data, considerando l'ordine:
 * north -> east -> south -> west.  
 * Ad esempio, la successiva di "north" è "east", quella di "east" è "south", ecc.
 */
private String getNext(String direction) {
    switch(direction.toLowerCase()){
        case "north": return "west";
        case "east":  return "north";
        case "south": return "east";
        case "west":  return "south";
        default:      return "";
    }
}


private boolean containsRequest(String vehicleID) {
    for (Request req : pendingRequests) {
        if (req.vehicleID.equalsIgnoreCase(vehicleID))
            return true;
    }
    for (Request req : currentRequests) {
        if (req.vehicleID.equalsIgnoreCase(vehicleID))
            return true;
    }
    return false;
}

private void sendEnd(String vehicleID) {
    ACLMessage endMsg = new ACLMessage(ACLMessage.INFORM);
    endMsg.addReceiver(new jade.core.AID(vehicleID, jade.core.AID.ISLOCALNAME));
    endMsg.setContent("END");
    send(endMsg);
    System.out.println("IntersectionAgent: inviato END a " + vehicleID);
}

private void sendGo(String vehicleID) {
        ACLMessage goMsg = new ACLMessage(ACLMessage.CONFIRM);
        goMsg.addReceiver(new jade.core.AID(vehicleID, jade.core.AID.ISLOCALNAME));
        goMsg.setContent("GO");
        send(goMsg);
        System.out.println("IntersectionAgent: inviato GO a " + vehicleID);
    }
}
