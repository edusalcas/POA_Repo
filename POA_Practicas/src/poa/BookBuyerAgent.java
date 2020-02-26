package poa;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;

@SuppressWarnings("serial")
public class BookBuyerAgents extends Agent {

	// The title of the book to buy
	private String targetBookTitle;
	// The list of know seller agents
	private AID[] sellerAgents = { new AID("seller1", AID.ISLOCALNAME), new AID("seller2", AID.ISLOCALNAME) };

	public void setup() {
		System.out.println("Hello! Buyer-agent" + getAID().getName() + "is ready.");

		// Get the title of the book to buy as a start-ip argument
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			targetBookTitle = (String) args[0];
			System.out.println("Trying to buy " + targetBookTitle);
			addBehaviour(new TickerBehaviour(this, 20000) {
				
				@Override
				protected void onTick() {
					System.out.println("Sending information to any seller");
				}
			});

		} else {
			System.out.println("No book titile specified");
			doDelete();
		}
		
	}
	
	@Override
	protected void takeDown() {
		System.out.println("Buyer-agent " + getAID().getName() + "terminating...");
		super.takeDown();
	}


}
