import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import Utility.SumoConnector;
import Utility.Environment;
import Utility.Intersection;
import java.util.List;

public class TestSimulation {

    public static void main(String[] args) {
        // Avvia il runtime JADE e crea il main container
        Runtime rt = Runtime.instance();
        rt.setCloseVM(true);
        Profile p = new ProfileImpl();
        AgentContainer mainContainer = rt.createMainContainer(p);
        
        try {

            // 0. Carica la mappa da un file XML
            Environment.loadMap("SDAI_Project\\src\\SUMO_config\\cross.net.xml");

            // 1. Avvia la connessione a SUMO
            SumoConnector.connect("SDAI_Project\\src\\SUMO_config\\cross.sumocfg", 12345);

            // 2. Crea un agente IntersectionAgent per ogni incrocio definito in Environment
            List<Intersection> incroci = Environment.getIntersections();
            for (Intersection inc : incroci) {
                // L'agentName è inc.getAgentName(), ad es. "IntersectionAgent_C2"
                AgentController intersectionAgent = mainContainer.createNewAgent(
                    inc.getAgentName(),
                    "IntersectionAgent",
                    null
                );
                intersectionAgent.start();
            }
            
            // 3. Crea un agente SimStepAgent per gestire i passi di simulazione
            mainContainer.createNewAgent("SimStepAgent", "SimStepAgent", null).start();

            AgentController vehicle = mainContainer.createNewAgent("v1", "VehicleAgent", null);
            vehicle.start();
            AgentController vehicle1 = mainContainer.createNewAgent("v2", "VehicleAgent", null);
            vehicle1.start();
            AgentController vehicle2 = mainContainer.createNewAgent("v3", "VehicleAgent", null);
            vehicle2.start();
            AgentController vehicle3 = mainContainer.createNewAgent("v4", "VehicleAgent", null);    
            vehicle3.start();
            AgentController vehicle4 = mainContainer.createNewAgent("v5", "VehicleAgent", null);
            vehicle4.start();
            AgentController vehicle5 = mainContainer.createNewAgent("v6", "VehicleAgent", null);
            vehicle5.start();
            AgentController vehicle6 = mainContainer.createNewAgent("v7", "VehicleAgent", null);
            vehicle6.start();
            AgentController vehicle7 = mainContainer.createNewAgent("v8", "VehicleAgent", null);
            vehicle7.start();
            AgentController vehicle8 = mainContainer.createNewAgent("v9", "VehicleAgent", null);
            vehicle8.start();
            AgentController vehicle9 = mainContainer.createNewAgent("v10", "VehicleAgent", null);
            vehicle9.start();
            AgentController vehicle10 = mainContainer.createNewAgent("v11", "VehicleAgent", null);
            vehicle10.start();
            AgentController vehicle11 = mainContainer.createNewAgent("v12", "VehicleAgent", null);
            vehicle11.start();
            AgentController vehicle12 = mainContainer.createNewAgent("v13", "VehicleAgent", null);
            vehicle12.start();
            AgentController vehicle13 = mainContainer.createNewAgent("v14", "VehicleAgent", null);
            vehicle13.start();
            AgentController vehicle14 = mainContainer.createNewAgent("v15", "VehicleAgent", null);
            vehicle14.start();
            AgentController vehicle15 = mainContainer.createNewAgent("v16", "VehicleAgent", null);
            vehicle15.start();
            AgentController vehicle16 = mainContainer.createNewAgent("v17", "VehicleAgent", null);
            vehicle16.start();
            AgentController vehicle17 = mainContainer.createNewAgent("v18", "VehicleAgent", null);
            vehicle17.start();
            AgentController vehicle18 = mainContainer.createNewAgent("v19", "VehicleAgent", null);
            vehicle18.start();

            AgentController vehicleAggressive1 = mainContainer.createNewAgent("vA1", "VehicleAggressiveAgent", null);
            vehicleAggressive1.start();

            // 5. Chiudi la connessione a SUMO
            //SumoConnector.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
