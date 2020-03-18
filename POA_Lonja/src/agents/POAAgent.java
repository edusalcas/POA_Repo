package agents;

import utils.AgentLoggerWrapper;
import jade.core.Agent;

public class POAAgent extends Agent {

	private static final long serialVersionUID = 1L;
	private AgentLoggerWrapper logger;
	
	public void setup() {
		this.logger = new AgentLoggerWrapper(this);
	}
	
	public AgentLoggerWrapper getLogger() {
		return this.logger;
	}
	
	public void takeDown() {
		super.takeDown();
		this.logger.close();
	}
}
