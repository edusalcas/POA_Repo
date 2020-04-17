package agents.fishship;

import agents.POAAgent;
import agents.fishmarket.FishMarketAgentConfig;
import jade.domain.DFService;
import jade.domain.FIPAException;

public class FishShipAgent extends POAAgent{
	
	private static final long serialVersionUID = 1L;

	// ---------------------------------//
		// ------------Funciones------------//
		// ---------------------------------//
		public void setup() {
			super.setup();
			Object[] args = getArguments();
			if (args != null && args.length == 1) {
				String configFile = (String) args[0];
				FishMarketAgentConfig config = initAgentFromConfigFile(configFile);

				if (config != null) {
					init(config);
				}
			} else {
				this.getLogger().info("ERROR", "Requiere fichero de cofiguración.");
				doDelete();
			}
		}

		@Override
		public void takeDown() {
			// Deregister from the yellow pages
			try {
				DFService.deregister(this);
			} catch (FIPAException fe) {
				fe.printStackTrace();
			}

			// Printout a dismissal message
			System.out.println("FishMarket-agent " + getAID().getName() + " terminating.");
			super.takeDown();
		}
	
}
