import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class IntersectionAgent extends Agent {

    private final long TIMEOUT = 15000; // Timeout in millisecondi
    // Lista di richieste pendenti
    private List<Request> pendingRequests = new ArrayList<>();
    // Richiesta attualmente autorizzata
    private Request currentRequest = null;

    private class Request {
        String vehicleID;
        String arrivalLane;
        String turningIntention;
        long timestamp;

        public Request(String vehicleID, String arrivalLane, String turningIntention, long timestamp) {
            this.vehicleID = vehicleID;
            this.arrivalLane = arrivalLane.toLowerCase();
            this.turningIntention = turningIntention.toLowerCase();
            this.timestamp = timestamp;
        }
        
        @Override
        public String toString() {
            return "[" + vehicleID + ", " + arrivalLane + ", " + turningIntention + ", " + timestamp + "]";
        }
    }

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " avviato come IntersectionAgent.");
        addBehaviour(new ProcessMessagesBehaviour());
        addBehaviour(new QueueProcessingBehaviour(this, 300));
    }

    // Comportamento che riceve tutti i messaggi: sia REQUEST_PASS che PASSED
    private class ProcessMessagesBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                String content = msg.getContent().trim();
                // Gestisci il messaggio PASSED
                if (content.startsWith("PASSED,")) {
                    String passedVehicle = content.substring("PASSED,".length()).trim();
                    if (currentRequest != null && currentRequest.vehicleID.equalsIgnoreCase(passedVehicle)) {
                        System.out.println("IntersectionAgent: " + passedVehicle + " ha attraversato l'incrocio. Resettando la richiesta attuale.");
                        currentRequest = null;
                    }
                }
                // Gestisci le richieste di passaggio
                else if (content.startsWith("REQUEST_PASS,")) {
                    String[] parts = content.split(",");
                    if (parts.length < 5) {
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
                    Request newReq = new Request(vehicleID, arrivalLane, turningIntention, timestamp);
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
            Collections.sort(pendingRequests, Comparator.comparingLong(r -> r.timestamp));
            
            if (currentRequest != null && now - currentRequest.timestamp > TIMEOUT) {
                System.out.println("IntersectionAgent: Timeout per " + currentRequest.vehicleID);
                sendEnd(currentRequest.vehicleID);
                currentRequest = null;
            }
            
            if (currentRequest == null && !pendingRequests.isEmpty()) {
                currentRequest = pendingRequests.remove(0);
                sendGo(currentRequest.vehicleID);
                System.out.println("IntersectionAgent: " + currentRequest.vehicleID + " autorizzato (FCFS).");
            }
            
            if (currentRequest != null) {
                Request candidate = null;
                for (Request req : pendingRequests) {
                    if (isRightOf(req.arrivalLane, currentRequest.arrivalLane)) {
                        candidate = req;
                        break;
                    }
                }
                if (candidate != null) {
                    System.out.println("IntersectionAgent: " + candidate.vehicleID + " (da " + candidate.arrivalLane +
                        ") ha la precedenza rispetto a " + currentRequest.vehicleID +
                        " (da " + currentRequest.arrivalLane + ").");
                    sendStop(currentRequest.vehicleID);
                    pendingRequests.remove(candidate);
                    currentRequest = candidate;
                    sendGo(currentRequest.vehicleID);
                }
            }
        }
    }

    private boolean containsRequest(String vehicleID) {
        for (Request req : pendingRequests) {
            if (req.vehicleID.equalsIgnoreCase(vehicleID))
                return true;
        }
        if (currentRequest != null && currentRequest.vehicleID.equalsIgnoreCase(vehicleID))
            return true;
        return false;
    }

    private boolean isRightOf(String laneNew, String laneCurrent) {
        return laneNew.equalsIgnoreCase(getRightLane(laneCurrent));
    }

    private String getRightLane(String lane) {
        switch(lane.toLowerCase()) {
            case "north": return "east";
            case "east":  return "south";
            case "south": return "west";
            case "west":  return "north";
            default: return "";
        }
    }

    private void sendStop(String vehicleID) {
        ACLMessage stopMsg = new ACLMessage(ACLMessage.INFORM);
        stopMsg.addReceiver(new jade.core.AID(vehicleID, jade.core.AID.ISLOCALNAME));
        stopMsg.setContent("STOP");
        send(stopMsg);
        System.out.println("IntersectionAgent: inviata STOP a " + vehicleID);
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
