import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class TestSimulation {
    
    public static void main(String[] args) {
        // Avvia il runtime JADE e crea il main container
        Runtime rt = Runtime.instance();
        rt.setCloseVM(true);
        Profile p = new ProfileImpl();
        AgentContainer mainContainer = rt.createMainContainer(p);
        
        try {
            // 1. Avvia l'EnvironmentAgent per centralizzare la mappa
            AgentController envAgent = mainContainer.createNewAgent("EnvironmentAgent", "EnvironmentAgent", null);
            envAgent.start();
            
            // Attendi un attimo per permettere all'ambiente di inizializzarsi
            Thread.sleep(1000);
            
            // 2. Avvia l'IntersectionAgent (l'ID qui contiene anche le coordinate dummy, ad esempio "IntersectionAgent_5_5")
            AgentController interAgent = mainContainer.createNewAgent("IntersectionAgent_5_5", "IntersectionAgent", null);
            interAgent.start();
            
            // Attendi un attimo per permettere all'incrocio di inizializzarsi
            Thread.sleep(1000);
            
            // ---------------- Scenario 1 ----------------
            // Creiamo due veicoli che partono da punti opposti.
            // Ad esempio:
            // - Vehicle1 partirà da sinistra e andrà verso destra.
            // - Vehicle2 partirà da destra e andrà verso sinistra.
            // Output atteso (log):
            //   I log dovrebbero mostrare che entrambi i veicoli calcolano la propria rotta,
            //   si muovono lungo la strada senza conflitti e non inviano richieste di prenotazione simultanee
            //   se non necessario (perché percorrono corsie differenti).
            AgentController vehicle1 = mainContainer.createNewAgent("Vehicle1", "VehicleAgent", null);
            vehicle1.start();
            
            AgentController vehicle2 = mainContainer.createNewAgent("Vehicle2", "VehicleAgent", null);
            vehicle2.start();
            
            // Attendi qualche secondo per osservare lo scenario 1
            Thread.sleep(10000);
            
            // ---------------- Scenario 2 ----------------
            // Per testare le regole di incrocio (precedenza, FCFS, priorità a destra),
            // possiamo creare due veicoli che si dirigono entrambi verso l'incrocio nello stesso slot temporale.
            // In un test controllato, si potrebbero parametrizzare i messaggi di richiesta per avere
            // tempi, corsie e priorità specifici. Per questo esempio, utilizziamo ancora VehicleAgent,
            // sapendo che essi invieranno richieste di prenotazione basate sulla loro rotta casuale.
            // Output atteso (log):
            //   - Verrà stampato il messaggio dell'IntersectionAgent che riceve due richieste conflittuali.
            //   - A seconda delle regole (priority-based, precedenza a destra e FCFS),
            //     una richiesta verrà confermata e l'altra rifiutata (con i relativi log).
            AgentController vehicle3 = mainContainer.createNewAgent("Vehicle3", "VehicleAgent", null);
            vehicle3.start();
            
            AgentController vehicle4 = mainContainer.createNewAgent("Vehicle4", "VehicleAgent", null);
            vehicle4.start();
            
            // Attendi ancora per osservare lo scenario 2
            Thread.sleep(15000);
            
            // I test possono essere ulteriormente estesi creando altri scenari,
            // oppure parametrizzando direttamente le rotte dei veicoli per simulazioni più controllate.
            
        } catch (StaleProxyException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
