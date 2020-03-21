package agents.buyer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.yaml.snakeyaml.Yaml;

import agents.POAAgent;
import agents.seller.Lot;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.AchieveREInitiator;

public class BuyerAgent extends POAAgent {

	private static final long serialVersionUID = 1L;

	private AID lonjaAgent = new AID("lonja", AID.ISLOCALNAME);

	private float budget;

	private boolean lineaCredito;

	private List<Lot> lots;

	public void setup() {
		super.setup();

		Object[] args = getArguments();
		if (args != null && args.length == 1) {
			String configFile = (String) args[0];
			BuyerAgentConfig config = initAgentFromConfigFile(configFile);

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

	private BuyerAgentConfig initAgentFromConfigFile(String fileName) {
		BuyerAgentConfig config = null;
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

	private void init(BuyerAgentConfig config) {
		System.out.println("Soy el agente comprador " + this.getName());
		
		lots = new LinkedList<Lot>();
		
		// Registramos el agente comprador en las p�ginas amarillas
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("comprador");
		sd.setName("JADE-Lonja");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException e) {
			e.printStackTrace();
		}

		// Introducimos los valores de configuraci�n en nuestro agente
		this.budget = config.getBudget();

		// A�adimos los Behaviours

		addBehaviour(new RequestAdmision());
		
		// Apertura de cr�dito
		ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		request.addReceiver(lonjaAgent);
		request.setContent(Float.toString(budget));
		request.setConversationId("apertura-credito");
		while (lineaCredito != true) {
			addBehaviour(new AchieveREInitiator(this, request) {
				private String estado;

				@Override
				protected void handleInform(ACLMessage inform) {
					// getLogger().info(behaviour, msg);
					lineaCredito = true;
				}

			});
		}

		// Retirada Compras
		request = new ACLMessage(ACLMessage.REQUEST);
		request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		request.addReceiver(lonjaAgent);
		request.setConversationId("retirar-compras");
		addBehaviour(new AchieveREInitiator(this, request) {
			@Override
			protected void handleInform(ACLMessage inform) {
				try {
					List<Lot> lotes = (List<Lot>) inform.getContentObject();
					lots = lotes;
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
				
			}
		});

	}
	
	/*
	 * Clase privada que se encarga del registro del comprador, le manda un mensaje
	 * tipo request y el RAC le responde si se le ha registrado correctamente o no
	 */
	private class RequestAdmision extends Behaviour {
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
				req.setConversationId("admision-comprador");
				req.setReplyWith("req" + System.currentTimeMillis()); // Unique value

				myAgent.send(req);
				// Prepare the template
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("admision-comprador"),
						MessageTemplate.MatchInReplyTo(req.getReplyWith()));
				step = 1;
				break;
			case 1:
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// Registro exitoso
						getLogger().info("RequestAdmisionComprador", "Register Succeed");
						;
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

}
