package agents.fishmarket;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.yaml.snakeyaml.Yaml;

import agents.POAAgent;
import agents.seller.Lot;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
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
	private HashMap<Integer, List<Subscription>> lines;
	private HashMap<Integer, List<Lot>> lineLots;

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
		lines = new HashMap<Integer, List<Subscription>>();
		lineLots = new HashMap<Integer, List<Lot>>();

		lines.put(1, new ArrayList<Subscription>());
		lineLots.put(1, new ArrayList<Lot>());

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

		// (protocolo-registro-vendedor) El RAV recibe la petición de registro del RV
		addBehaviour(new RequestRegistroVendedor());
		// (protocolo-deposito) El RRV recibe la petición de hacer un deposito de
		// capturas del RV
		addBehaviour(new RequestDepositoCaptura());

		// (protocolo-admision-comprador)
		addBehaviour(new RequestAdmisionComprador());

		// (protocolo-apertura-credito)
		MessageTemplate mt = MessageTemplate.and(
				AchieveREResponder.createMessageTemplate(FIPANames.InteractionProtocol.FIPA_REQUEST),
				MessageTemplate.MatchConversationId("apertura-credito"));
		this.addBehaviour(new AchieveREResponder(this, mt) {
			private static final long serialVersionUID = 1L;

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
			private static final long serialVersionUID = 1L;

			// TODO
			@Override
			protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response)
					throws FailureException {
				// TODO Actualizar tambi�n el cr�dito disponible
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

		// Protocolo Subscripci�n L�nea-Venta
		mt = MessageTemplate.and(SubscriptionResponder.createMessageTemplate(ACLMessage.SUBSCRIBE),
				MessageTemplate.MatchConversationId("subs-linea_venta"));
		addBehaviour(new SubscriptionResponder(this, mt) {
			private static final long serialVersionUID = 1L;

			@Override
			protected ACLMessage handleSubscription(ACLMessage subscription)
					throws NotUnderstoodException, RefuseException {
				ACLMessage response;

				if (subscriptionManager.register(createSubscription(subscription))) {
					response = subscription.createReply();
					response.setPerformative(ACLMessage.AGREE);

					int linea = Integer.parseInt(subscription.getContent());

					if (lines.get(linea).size() > 1 && lineLots.get(linea).size() > 3)
						iniciarLineaVenta(linea);

				} else {
					response = subscription.createReply();
					response.setPerformative(ACLMessage.REFUSE);
					response.setContent("Ha habido un error en el proceso de suscripcion");
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

	private void iniciarLineaVenta(int linea) {
		getLogger().info("Inicio subasta", "Ha dado comienzo la subasta de la linea " + linea);
		addBehaviour(new SubastasLineaVentas(linea));
	}

	/*
	 * Clase privada que se encarga de recibir los mensajes tipo request del
	 * vendedor para registrarse en la lonja. En caso de que el vendedor aun no este
	 * registrado, se le registrara. De esta comunicación se encarga el RAV.
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
	 * vendedor aun no este registrado, se le registrara, esta operación no se podrá
	 * hacer. De esta comunicación se encarga el RRV.
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

					// TODO Seleccionamos aleatoriamente la linea de venta para ese lote
					int randomLine = 1;
					lineLots.get(randomLine).add(lot);
					if (lines.get(randomLine).size() > 1 && lineLots.get(randomLine).size() > 3)
						iniciarLineaVenta(randomLine);
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
	 * este registrado, se le registrara. De esta comunicación se encarga el RAC.
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

	/*
	 * Clase privada necesaria para el protocolo suscribir en linea de venta,
	 * encargada de la función de suscribir y desuscribir a un agente
	 */
	private class Manager implements SubscriptionManager {

		@Override
		public boolean deregister(Subscription arg0) throws FailureException {
			int linea = Integer.parseInt(arg0.getMessage().getContent()); // Linea a la que se quiere suscribir
			AID sender = arg0.getMessage().getSender(); // Agente que se quiere suscribir

			// Si la linea existe, desuscribir al agente
			if (lines.containsKey(linea))
				for (Subscription s : lines.get(linea))
					if (s.getMessage().getSender().equals(sender)) {
						lines.get(linea).remove(s);
						return true;
					}

			return false;
		}

		@Override
		public boolean register(Subscription arg0) throws RefuseException, NotUnderstoodException {
			int linea = Integer.parseInt(arg0.getMessage().getContent()); // Linea a la que se quiere suscribir
			AID sender = arg0.getMessage().getSender(); // Agente que se quiere suscribir

			// Si no existe la linea, se devuelve false
			if (!lines.containsKey(linea))
				return false;

			// Si existe, se cuemprueba que no este suscrito ya a ninguna linea
			for (int lv : lines.keySet())
				for (Subscription s : lines.get(lv))
					if (s.getMessage().getSender().equals(sender))
						return false;

			// Si no esta suscrito, se suscribe a la linea de venta
			lines.get(linea).add(arg0);
			return true;
		}

	}
	// End of inner class Manager

	private class SubastasLineaVentas extends Behaviour {

		private static final long serialVersionUID = 1L;
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;
		private int lineaVentas = 0;
		Lot lote = null;
		private final int TIMEOUT = 1000;

		private long t0;

		public SubastasLineaVentas(int lineaVentas) {
			this.lineaVentas = lineaVentas;
		}

		@Override
		public void action() {
			if (!lineLots.get(lineaVentas).isEmpty()) {
				switch (step) {
				case 0:
					// Cogemos el siguiente lote a subastar
					lote = lineLots.get(lineaVentas).get(0);

					// Notificas a todos los agentes suscritos en esa linea
					notificarLinea(lineaVentas, lote);
					t0 = System.currentTimeMillis();

					step = 1;
					break;
				case 1:
					mt = MessageTemplate.and(
							AchieveREResponder.createMessageTemplate(FIPANames.InteractionProtocol.FIPA_REQUEST),
							MessageTemplate.MatchConversationId("realizar-puja-" + lote.getID()));
					ACLMessage reply = getAgent().receive(mt);
					if (reply != null) {
						// Borra el lote de la lina de ventas
						lineLots.get(lineaVentas).remove(0);
						// Se añade el lote a la lista de lotes del comprador
						// TODO
						// Se actualiza la linea de credito del comprador
						// TODO
						// Pagar al vendedor el precio de reserva
						pagarVendedor(lote);
						// Se responde con un inform al agente que ha realizado la puja
						ACLMessage response = reply.createReply();
						response.setContent("OK");
						response.setPerformative(ACLMessage.INFORM);
						getAgent().send(response);

						// Volvemos al primer paso
						step = 0;

					} else {

						if (System.currentTimeMillis() - t0 >= TIMEOUT) {
							if (lote.getPrecio() == lote.getPrecioReserva()) {
								getLogger().info("Subasta lote: " + lote.getID(),
										"No se ha vendido el lote: " + lote.toString());
								// Borrar el lote de la linea de ventas
								lineLots.get(lineaVentas).remove(0);
								// Añadir el lote a la lista de reservas
								// TODO
								// Pagar al vendedor
								pagarVendedor(lote);
							} else {
								lote.setPrecio(lote.getPrecio() - 1.0f);
							}
							step = 0;
						} else {
							block(1000);
						}

					}

					break;
				}
			}

		}

		@Override
		public boolean done() {
			return lineLots.get(lineaVentas).isEmpty();
		}

	}
	// End of inner class SubastasLineaVentas

	/*
	 * Clase privada que se encarga del registro del vendedor, le manda un mensaje
	 * tipo request y el RAV le responde si se le ha registrado correctamente o no
	 */
	private class RequestCobro extends Behaviour {
		private static final long serialVersionUID = 1L;
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;
		private AID vendedor;
		private float dinero;

		public RequestCobro(AID vendedor, float dinero) {
			this.vendedor = vendedor;
			this.dinero = dinero;
		}

		@Override
		public void action() {
			switch (step) {
			case 0:
				// Enviar request al agente lonja con rol RAV
				ACLMessage req = new ACLMessage(ACLMessage.REQUEST);

				req.addReceiver(vendedor);
				req.setConversationId("cobro-" + vendedor);
				req.setReplyWith("req" + System.currentTimeMillis()); // Unique value
				req.setContent(Float.toString(dinero));

				myAgent.send(req);
				// Prepare the template
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("cobro-" + vendedor),
						MessageTemplate.MatchInReplyTo(req.getReplyWith()));
				step = 1;
				break;
			case 1:
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// Registro exitoso
						getLogger().info("Protocolo-Cobro", "Agente " + vendedor.getLocalName() + " acepta el cobro");
					} else {
						// Fallo en el registro
						getLogger().info("Protocolo-Cobro", reply.getContent());
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

	private void pagarVendedor(Lot lote) {
		AID vendedor = getVendedor(lote);

		sellerAgents.get(vendedor).remove(lote);

		addBehaviour(new RequestCobro(vendedor, lote.getPrecioReserva()));

	}

	private AID getVendedor(Lot lote) {
		for (AID vendedor : sellerAgents.keySet())
			if (sellerAgents.get(vendedor).contains(lote))
				return vendedor;

		return null;
	}

	private void notificarLinea(int lv, Lot lote) {
		for (Subscription s : lines.get(lv)) {

			ACLMessage notification = s.getMessage().createReply();
			notification.setPerformative(ACLMessage.INFORM);
			// Poner como contenido el lote que se subasta (tipo, cantidad, precio)

			try {
				notification.setContentObject(lote);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			s.notify(notification);
		}
	}
}
