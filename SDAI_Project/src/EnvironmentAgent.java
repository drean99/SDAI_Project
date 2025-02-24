import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import Utility.MatrixMap;
import Utility.SumoConnector;

public class EnvironmentAgent extends Agent {
    // Istanza centralizzata della mappa, accessibile da tutti gli agenti
    public static MatrixMap sharedMap;

    protected void setup() {
        // Registra l'agente nel Directory Facilitator (DF)
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Environment");
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        // Inizializza la mappa (es. 10x10) secondo la logica definita in MatrixMap
        sharedMap = new MatrixMap(10, 10);
        System.out.println(getLocalName() + " avviato, mappa inizializzata:");
        sharedMap.printMap();
        
        // Connetti a SUMO tramite TraCI (modifica host/port se necessario)
        try {
            SumoConnector.connect("localhost", 8813); // 8813 è la porta di default per SUMO
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // Chiudi la connessione SUMO
        SumoConnector.close();
        System.out.println(getLocalName() + " terminato.");
    }
}
