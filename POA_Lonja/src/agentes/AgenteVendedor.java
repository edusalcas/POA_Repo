package agentes;

import gui.GuiComprador;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

@SuppressWarnings("serial")
public class AgenteVendedor extends Agent {

	// The GUI by means of which the user can add books in the catalogue
	private GuiComprador myGui;

	private AID lonjaAgent = new AID("lonja", AID.ISLOCALNAME);

	// Put agent initializations here
	@Override
	protected void setup() {
		System.out.println("Soy el agente vendedor " + this.getName());

		// Create and show the GUI
		myGui = new GuiComprador(this);
		myGui.showGui();

		// Register the book-selling service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("vendedor");
		sd.setName("JADE-Lonja");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		addBehaviour(new RequestRegistro());
	}

	// Put agent clean-up operations here
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// Printout a dismissal message
		System.out.println("Seller-agent " + getAID().getName() + " terminating.");
		super.takeDown();
	}

	public void nuevaMercancia(String title, int cantidad) {
		System.out.println("Nuevo paquete de mercancia con " + cantidad + "kg de " + title);
	}

	/*
	 * Clase privada que se encarga del registro del vendedor, le manda un mensaje tipo
	 * request y el RAV le responde si se le ha registrado correctamente o no
	 */
	private class RequestRegistro extends Behaviour {
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;

		@Override
		public void action() {
			switch (step) {
			case 0:
				// Enviar request al agente lonja con rol RAV
				ACLMessage req = new ACLMessage(ACLMessage.REQUEST);

				req.addReceiver(lonjaAgent);
				req.setConversationId("registro-vendedor");
				req.setReplyWith("req" + System.currentTimeMillis()); // Unique value

				myAgent.send(req);
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("registro-vendedor"),
						MessageTemplate.MatchInReplyTo(req.getReplyWith()));
				step = 1;
				break;
			case 1:
				// Receive all proposals/refusals from seller agents
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// Registro exitoso
						System.out.println("Registrado con exito");
						step = 2;
					} else {
						// Fallo en el registro
						System.out.println(reply.getContent());
					}

				} else {
					block();
				}
				break;
			}

		}

		@Override
		public boolean done() {
			return step == 2;
		}

	}
	// End of inner class RequestRegistro
}
