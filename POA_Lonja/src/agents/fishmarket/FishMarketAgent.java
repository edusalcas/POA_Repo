package agents.fishmarket;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import org.yaml.snakeyaml.Yaml;

import agents.POAAgent;
import agents.seller.Lot;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import jade.proto.FIPAProtocolNames;
import jade.proto.SubscriptionResponder;
import jade.proto.SubscriptionResponder.Subscription;
import jade.proto.SubscriptionResponder.SubscriptionManager;

public class FishMarketAgent extends POAAgent {

	private static final long serialVersionUID = 1L;
	// The list of know seller agents
	private HashMap<AID, List<Lot>> sellerAgents;

	private ArrayList<AID> buyerAgents;
	private HashMap<AID, Float> lineasCredito;
	private HashMap<AID, List<Lot>> lotesComprador;
	private Manager subscriptionManager;
	private HashMap<Integer, List<Subscription>> lanes;

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
			this.getLogger().info("ERROR", "Requiere fichero de cofiguraci贸n.");
			doDelete();
		}
	}

	private FishMarketAgentConfig initAgentFromConfigFile(String fileName) {
		FishMarketAgentConfig config = null;
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

	private void init(FishMarketAgentConfig config) {
		System.out.println("Soy el agente lonja " + this.getName());

		// Initialize variables
		sellerAgents = new HashMap<AID, List<Lot>>();
		buyerAgents = new ArrayList<AID>();
		lineasCredito = new HashMap<AID, Float>();
		lotesComprador = new HashMap<AID, List<Lot>>();
		lanes = new HashMap<Integer, List<Subscription>>();
		subscriptionManager = new Manager();

		// Register the fish-market service in the yellow pages
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

		// (protocolo-registro-vendedor) El RAV recibe la petici贸n de registro del RV
		addBehaviour(new RequestRegistroVendedor());
		// (protocolo-deposito) El RRV recibe la petici贸n de hacer un deposito de
		// capturas del RV
		addBehaviour(new RequestDepositoCaptura());

		// (protocolo-admision-comprador)
		addBehaviour(new RequestAdmisionComprador());

		// (protocolo-apertura-credito)
		MessageTemplate mt = MessageTemplate.and(
				AchieveREResponder.createMessageTemplate(FIPANames.InteractionProtocol.FIPA_REQUEST),
				MessageTemplate.MatchConversationId("apertura-credito"));
		this.addBehaviour(new AchieveREResponder(this, mt) {
			@Override
			protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {
				AID sender = request.getSender();
				if (!buyerAgents.contains(sender)) {
					ACLMessage response = request.createReply();
					response.setPerformative(ACLMessage.REFUSE);
					response.setContent("not registered");
					return response;
				}
				ACLMessage response = request.createReply();
				response.setPerformative(ACLMessage.AGREE);
				return response;
			}

			@Override
			protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response)
					throws FailureException {
				lineasCredito.put(request.getSender(), Float.valueOf(request.getContent()));
				ACLMessage informDone = request.createReply();
				informDone.setPerformative(ACLMessage.INFORM);
				informDone.setContent("OK");
				return informDone;
			}
		});

		// protocolo retirada compras
		mt = MessageTemplate.and(AchieveREResponder.createMessageTemplate(FIPANames.InteractionProtocol.FIPA_REQUEST),
				MessageTemplate.MatchConversationId("retirar-compras"));
		addBehaviour(new AchieveREResponder(this, mt) {
			// TODO
			@Override
			protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response)
					throws FailureException {
				// TODO Actualizar tambi閚 el cr閐ito disponible
				List<Lot> content = lotesComprador.get(request.getSender());
				ACLMessage informDone = request.createReply();
				informDone.setPerformative(ACLMessage.INFORM);
				try {
					informDone.setContentObject((Serializable) content);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return informDone;
			}
		});

		// Protocolo Subscripci髇 L韓ea-Venta
		mt = MessageTemplate.and(SubscriptionResponder.createMessageTemplate(ACLMessage.SUBSCRIBE),
				MessageTemplate.MatchConversationId("subs-linea_venta"));
		addBehaviour(new SubscriptionResponder(this, mt) {
			@Override
			protected ACLMessage handleSubscription(ACLMessage subscription)
					throws NotUnderstoodException, RefuseException {
				ACLMessage response;
				if(subscriptionManager.register(createSubscription(subscription))) {
					response = subscription.createReply();
					response.setPerformative(ACLMessage.AGREE);
				} else {
					response = subscription.createReply();
					response.setPerformative(ACLMessage.REFUSE);
					response.setContent("Ya est醩 suscrito a una l韓ea de venta");
				}				
				return response;
			}
			
			@Override
			protected ACLMessage handleCancel(ACLMessage cancel) throws FailureException {
				// TODO 
				return super.handleCancel(cancel);
			}
		});

	}

	/*
	 * Clase privada que se encarga de recibir los mensajes tipo request del
	 * vendedor para registrarse en la lonja. En caso de que el vendedor aun no este
	 * registrado, se le registrara. De esta comunicaci贸n se encarga el RAV.
	 */
	private class RequestRegistroVendedor extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		public void action() {
			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("registro-vendedor"),
					MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
			ACLMessage msg = myAgent.receive(mt);
			AID sender;
			if (msg != null) {
				// Request Message received. Process it.
				ACLMessage reply = msg.createReply();
				sender = msg.getSender();

				if (!sellerAgents.containsKey(sender)) {
					// Seller can be registered
					sellerAgents.put(sender, new ArrayList<Lot>());
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
	 * Clase privada que se encarga de recibir los mensajes tipo request del
	 * vendedor para hacer un deposito de una captura en la lonja. En caso de que el
	 * vendedor aun no este registrado, se le registrara, esta operaci贸n no se
	 * podr谩 hacer. De esta comunicaci贸n se encarga el RRV.
	 */
	private class RequestDepositoCaptura extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		public void action() {
			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("deposito-captura"),
					MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
			ACLMessage msg = myAgent.receive(mt);
			AID sender;

			if (msg != null) {
				// Request Message received. Process it.
				ACLMessage reply = msg.createReply();
				String[] content = msg.getContent().split(",");
				String type = content[0];
				float kg = Float.parseFloat(content[1]);

				sender = msg.getSender();

				if (sellerAgents.containsKey(sender)) {
					// Seller is registred
					Lot lot = new Lot(type, kg);
					sellerAgents.get(sender).add(lot);

					reply.setContent(msg.getContent());
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

	/*
	 * Clase privada que se encarga de recibir los mensajes tipo request del
	 * comprador para registrarse en la lonja. En caso de que el vendedor aun no
	 * este registrado, se le registrara. De esta comunicaci贸n se encarga el RAC.
	 */
	private class RequestAdmisionComprador extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		public void action() {
			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("admision-comprador"),
					MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
			ACLMessage msg = myAgent.receive(mt);
			AID sender;
			if (msg != null) {
				// Request Message received. Process it.
				ACLMessage reply = msg.createReply();
				sender = msg.getSender();

				if (!buyerAgents.contains(sender)) {
					// Seller can be registered
					buyerAgents.add(sender);
					lotesComprador.put(sender, new ArrayList<Lot>());
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

	private class Manager implements SubscriptionManager {

		@Override
		public boolean deregister(Subscription arg0) throws FailureException {
			if (lanes.containsKey(Integer.parseInt(arg0.getMessage().getContent()))) {
				for (Subscription s : lanes.get(Integer.parseInt(arg0.getMessage().getContent()))) {
					if (s.getMessage().getSender().equals(arg0.getMessage().getSender())) {
						lanes.get(Integer.parseInt(arg0.getMessage().getContent())).remove(s);
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public boolean register(Subscription arg0) throws RefuseException, NotUnderstoodException {
			if (!lanes.containsKey(Integer.parseInt(arg0.getMessage().getContent()))) {
				return false;
			}
			for (int lv : lanes.keySet()) {
				for (Subscription s : lanes.get(lv)) {
					if (s.getMessage().getSender().equals(arg0.getMessage().getSender())) {
						return false;
					}
				}
			}
			lanes.get(Integer.parseInt(arg0.getMessage().getContent())).add(arg0);
			return true;
		}

	}
	
	private void notificarLinea(int lv) {
		for(Subscription s : lanes.get(lv)) {
			ACLMessage notification = new ACLMessage();
			notification.setContent("patata");
			s.notify(notification);
		}
	}
}
