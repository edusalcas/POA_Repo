package agents.seller;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

import org.yaml.snakeyaml.Yaml;

import agents.POAAgent;

import gui.GuiVendedor;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class SellerAgent extends POAAgent {

	private static final long serialVersionUID = 1L;
	private final int PROB = 10;
	private GuiVendedor myGui;

	private AID lonjaAgent = new AID("lonja", AID.ISLOCALNAME);
	private ArrayList<Lot> lots;

	private float dinero; 
	
	public void setup() {
		super.setup();

		Object[] args = getArguments();
		if (args != null && args.length == 1) {
			String configFile = (String) args[0];
			SellerAgentConfig config = initAgentFromConfigFile(configFile);

			if (config != null) {
				init(config);
			} else {
				doDelete();
			}
		} else {
			getLogger().info("ERROR", "Requiere fichero de cofiguración.");
			doDelete();
		}
	}

	private void init(SellerAgentConfig config) {

		System.out.println("Soy el agente vendedor " + this.getName());

		// Create and show the GUI
		myGui = new GuiVendedor(this);
		myGui.showGui();

		// Register the selling-agent service in the yellow pages
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

		// Add the lots to the agent
		addLotsFromConfig(config);

		dinero = 0.0f;
		
		// (protocolo-registro-vendedor) El RAV recibe la petición de registro del RV
		addBehaviour(new RequestRegistro());

		// (protocolo-deposito) El RRV recibe la petición de hacer un deposito de
		// capturas del RV
		addBehaviour(new DepositoDeCaptura());
		
		// (protocolo-cobro)
		addBehaviour(new RecieveCobro());

	}

	private void addLotsFromConfig(SellerAgentConfig config) {
		lots = (ArrayList<Lot>) config.lots;
	}

	private SellerAgentConfig initAgentFromConfigFile(String fileName) {
		SellerAgentConfig config = null;
		try {
			Yaml yaml = new Yaml();
			InputStream inputStream;
			inputStream = new FileInputStream(fileName);
			config = yaml.load(inputStream);
			getLogger().info("initAgentFromConfigFile", config.toString());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return config;
	}

	public void nuevaMercancia(String type, float kg) {
		System.out.println("Nuevo paquete de mercancia con " + kg + "kg de " + type);

		Lot lot = new Lot(type, kg);
		lots.add(lot);
	}

	/*
	 * Clase privada que se encarga del registro del vendedor, le manda un mensaje
	 * tipo request y el RAV le responde si se le ha registrado correctamente o no
	 */
	private class RequestRegistro extends Behaviour {
		private static final long serialVersionUID = 1L;
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
				// Prepare the template
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("registro-vendedor"),
						MessageTemplate.MatchInReplyTo(req.getReplyWith()));
				step = 1;
				break;
			case 1:
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// Registro exitoso
						getLogger().info("RequestRegistroVendedor", "Register Succeed");
					} else {
						// Fallo en el registro
						System.out.println(reply.getContent());
					}
					step = 2;

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

	private class DepositoDeCaptura extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;

		@Override
		public void action() {
			if (!lots.isEmpty()) {
				switch (step) {
				case 0:

					Lot lot = lots.get(0);
					String type = lot.getType();
					float kg = lot.getKg();

					// Enviar request al agente lonja con rol RAV
					ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
					req.addReceiver(lonjaAgent);
					req.setConversationId("deposito-captura");
					req.setContent(type + "," + kg);
					req.setReplyWith("dep" + System.currentTimeMillis()); // Unique value

					myAgent.send(req);

					// Prepare the template to get response
					mt = MessageTemplate.and(MessageTemplate.MatchConversationId("deposito-captura"),
							MessageTemplate.MatchInReplyTo(req.getReplyWith()));

					step = 1;

					break;
				case 1:
					ACLMessage reply = myAgent.receive(mt);
					if (reply != null) {
						// Reply received
						if (reply.getPerformative() == ACLMessage.INFORM) {
							// Deposito de capturas exitoso
							String[] content = reply.getContent().split(",");
							String type1 = content[0];
							float kg1 = Float.parseFloat(content[1]);
							getLogger().info("DepositoDeCaptura", "Deposito de " + kg1 + "kg de " + type1 + " exitoso");

							lots.remove(0);
							step = 0;
						} else {
							// Fallo en el deposito de capturas
							System.out.println(reply.getContent());

							step = 0;
						}

					} else {
						block();
					}

					break;
				}
			}
		}

	}
	// End of inner class Deposito de Capturas

	private class RecieveCobro extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("cobro-" + myAgent.getAID()),
					MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// Request Message received. Process it.
				ACLMessage reply = msg.createReply();
				float precio = Float.parseFloat(msg.getContent());
				
				// Comprobar si queremos aceptar el pago
				// TODO De momento se cumple siempre
				Random rand = new Random();
				int randonResponse = rand.nextInt(100);
				if (randonResponse >= PROB) {
					// Aceptamos el pago
					reply.setPerformative(ACLMessage.INFORM);
					// Nos añadimos el dinero
					dinero += precio;
				} else {
					// Rechazamos el pago
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("No se acepta el dinero");
				}

				myAgent.send(reply);
			} else {
				block();
			}

		}

	}
	// End of inner class Deposito de Capturas
}
