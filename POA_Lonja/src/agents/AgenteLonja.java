package agents;

import java.util.ArrayList;
import java.util.Hashtable;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

@SuppressWarnings("serial")
public class AgenteLonja extends Agent {
	// The list of know seller agents
	private ArrayList<AID> sellerAgents;
	private Hashtable<String, Integer> captures;

	// Put agent initializations here
	@Override
	protected void setup() {
		System.out.println("Soy el agente lonja " + this.getName());

		sellerAgents = new ArrayList<AID>();
		captures = new Hashtable<String, Integer>();

		// Register the book-selling service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("lonja");
		sd.setName("JADE-Lonja");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// (protocolo-registro-vendedor) El RAV recibe la petición de registro del RV
		addBehaviour(new RequestRegistroVendedor());
		// (protocolo-deposito) El RRV recibe la petición de hacer un deposito de capturas del RV
		addBehaviour(new RequestDepositoCaptura());
	}

	@Override
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// Printout a dismissal message
		System.out.println("Lonja-agent " + getAID().getName() + " terminating.");

		super.takeDown();
	}

	/*
	 * Clase privada que se encarga de recibir los mensajes tipo request del vendedor
	 * para registrarse en la lonja. En caso de que el vendedor aun no este registrado,
	 * se le registrara. De esta comunicación se encarga el RAV.
	 */
	private class RequestRegistroVendedor extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = myAgent.receive(mt);
			AID sender;
			if (msg != null) {
				// Request Message received. Process it.
				ACLMessage reply = msg.createReply();
				sender = msg.getSender();

				if (!sellerAgents.contains(sender)) {
					// Seller can be registered
					sellerAgents.add(sender);
					reply.setPerformative(ACLMessage.INFORM);
				} else {
					// Seller cant be registered
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("Fallo en el registro");
				}

				myAgent.send(reply);
			} else {
				block();
			}
		}
	}
	// End of inner class RequestRegistroVendedor
	
	/*
	 * Clase privada que se encarga de recibir los mensajes tipo request del vendedor
	 * para hacer un deposito de una captura en la lonja. En caso de que el vendedor aun no este registrado,
	 * se le registrara, esta operación no se podrá hacer. De esta comunicación se encarga el RRV.
	 */
	private class RequestDepositoCaptura extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = myAgent.receive(mt);
			AID sender;
			if (msg != null) {
				// Request Message received. Process it.
				ACLMessage reply = msg.createReply();
				String[] content = msg.getContent().split(",");
				String title = content[0];
				int cantidad = Integer.parseInt(content[1]);
				
				sender = msg.getSender();

				if (sellerAgents.contains(sender)) {
					// Seller is registred
					captures.put(title, cantidad);
					reply.setPerformative(ACLMessage.INFORM);
				} else {
					// Seller isnt registered
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("Tienes que estar registrado para hacer un deposito");
				}

				myAgent.send(reply);
			} else {
				block();
			}
		}
	}
	// End of inner class RequestDepositoCaptura


}
