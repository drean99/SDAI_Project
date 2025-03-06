import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import Utility.SumoConnector;

public class SimStepAgent extends Agent {
    @Override
    protected void setup() {
        addBehaviour(new TickerBehaviour(this, 1000) { // ogni 1 secondo
            @Override
            protected void onTick() {
                int time = SumoConnector.getCurrentSimStep();
                System.out.println("Step di simulazione: " + time);
                SumoConnector.nextSimStep();
            }
        });
    }
}

