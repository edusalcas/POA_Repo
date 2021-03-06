package es.um.poa.agents.fishmarket;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.yaml.snakeyaml.Yaml;

import es.um.poa.agents.POAAgent;
import es.um.poa.agents.seller.Lot;
import jade.core.AID;
import jade.core.Agent;
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
import jade.lang.acl.UnreadableException;
import jade.proto.AchieveREResponder;
import jade.proto.SubscriptionResponder;
import jade.proto.SubscriptionResponder.Subscription;
import jade.proto.SubscriptionResponder.SubscriptionManager;
import es.um.poa.utils.MessageCreator;

/**
 * Clase que representa al agente lonja
 * 
 * @author Eduardo Salmeron Castaño Francisco Hita Ruiz
 */
public class FishMarketAgent extends POAAgent {

	// ---------------------------------//
	// ------------Variables------------//
	// ---------------------------------//
	private static final long serialVersionUID = 1L;

	private HashMap<AID, List<Lot>> sellerAgents; // Agentes vendedores y sus lotes
	private HashMap<AID, Float> lineasCredito; // Lineas de credito de cada comprador
	private HashMap<AID, List<Lot>> lotesComprador; // Lotes que cada comprador ha comprado
	private HashMap<Integer, List<Subscription>> lines; // Lineas de venta y los suscriptores de cada uno
	private HashMap<Integer, List<Lot>> lineLots; // Lotes en cada linea de venta
	private HashMap<Integer, Boolean> lineasAbiertas; // Indica si se ha iniciado una subasta en una linea de ventas
	private List<AID> buyerAgents; // Agentes compradores
	private List<Lot> lotsReserva; // Lotes que no se han vendido

	private float ingresos_actuales = 0f;

	private Manager subscriptionManager; // Manejador de las suscripciones por parte de los compradores a las lineas de
											// venta

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
			this.getLogger().info("ERROR", "Requiere fichero de cofiguracion.");
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

	/**
	 * Funcion encargada de inicializar el agente lonja
	 * 
	 * @param config parametros de configuracion
	 */
	private void init(FishMarketAgentConfig config) {
		// Anunciamos que el agente ha sido creado
		System.out.println("Soy el agente lonja " + this.getName());

		// Initialize variables
		sellerAgents = new HashMap<AID, List<Lot>>();
		buyerAgents = new ArrayList<AID>();
		lineasCredito = new HashMap<AID, Float>();
		lotesComprador = new HashMap<AID, List<Lot>>();
		lines = new HashMap<Integer, List<Subscription>>();
		lineLots = new HashMap<Integer, List<Lot>>();
		lotsReserva = new ArrayList<Lot>();
		lineasAbiertas = new HashMap<Integer, Boolean>();

		// Creamos una linea de ventas
		lines.put(1, new ArrayList<Subscription>());
		lineasAbiertas.put(1, false);
		lineLots.put(1, new ArrayList<Lot>());

		// Manejador de las siscripciones a las lineas de venta
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

		// Añadimos los Behaviours
		// (protocolo-registro-vendedor) El RAV recibe la petición de registro del RV
		addBehaviour(new RequestRegistroVendedor());
		// (protocolo-deposito) El RRV recibe la petición de hacer un deposito de
		// capturas del RV
		addBehaviour(new RequestDepositoCaptura());

		// (protocolo-admision-comprador)
		addBehaviour(new RequestAdmisionComprador());

		// (protocolo-apertura-credito)
		this.addBehaviour(new AperturaCreditoResponder(this, MessageCreator.msgAperturaCreditoResponder()));

		// (protocolo-retirada-compras)
		addBehaviour(new RetiradaComprasResponder(this, MessageCreator.msgRetiradaComprasResponder()));

		// (protocolo-subscripcion-linea-ventas)
		addBehaviour(new SuscripcionLineaVentasResponder(this, MessageCreator.msgSuscripcionLineaVentasResponder()));

	}

	// ---------------------------------//
	// -------Funciones privadas--------//
	// ---------------------------------//

	/**
	 * Funcion encargada de iniciar una linea de ventas
	 * 
	 * @param linea numero de la linea de ventas
	 */
	private void iniciarLineaVenta(int linea) {
		getLogger().info("SubastasLineaVentas",
				"La subasta de la linea de venta numero " + linea + " tendra comienzo en 5 segundos");
		addBehaviour(new SubastasLineaVentas(linea));
		lineasAbiertas.put(linea, true);
	}

	/**
	 * Funcion engargada de pagar al vendedor correspondiente el precio de reserva
	 * de un lote
	 * 
	 * @param lote lote vendido
	 */
	private void pagarVendedor(Lot lote) {
		AID vendedor = getVendedor(lote);

		sellerAgents.get(vendedor).remove(lote);

		addBehaviour(new RequestCobro(vendedor, lote.getPrecioReserva()));

	}

	/**
	 * Funcion engargada de devolver el vendedor correspondiente a un lote
	 * 
	 * @param lote lote del que se quiere saber el vendedor
	 * @return vendedor al que pertenecia el lote
	 */
	private AID getVendedor(Lot lote) {
		for (AID vendedor : sellerAgents.keySet())
			if (sellerAgents.get(vendedor).contains(lote))
				return vendedor;

		return null;
	}

	/**
	 * Funcion encargada de notificar la subasta de un lote en una linea de ventas
	 * 
	 * @param lv   numero de la linea de ventas
	 * @param lote lote que se subasta
	 */
	private void notificarLinea(int lv, Lot lote) {
		for (Subscription s : lines.get(lv)) {

			ACLMessage notification = s.getMessage().createReply();
			notification.setPerformative(ACLMessage.INFORM);
			// Poner como contenido el lote que se subasta (tipo, cantidad, precio)

			try {
				notification.setContentObject(lote);
			} catch (IOException e) {
				getLogger().info("Subasta Linea Venta", "No se ha podido a�adir el lote a un mensaje de notificacion");
				e.printStackTrace();
			}

			s.notify(notification);
		}
	}

	/**
	 * Funcion encargada de cerrar una linea de ventas y de notificar a todos los
	 * compradores que se encuentren en ella
	 * 
	 * @param lv numero de la linea de ventas
	 */
	private void cerrarLinea(int lv) {
		lineasAbiertas.put(lv, false);
		List<Subscription> subscripciones = lines.get(lv);
		lines.remove(lv);
		ArrayList<Integer> lineasVenta = new ArrayList<Integer>(lines.keySet());

		for (Subscription s : subscripciones) {

			ACLMessage notification = s.getMessage().createReply();
			try {
				notification.setContentObject((Serializable) lineasVenta);
				notification.setPerformative(ACLMessage.FAILURE);
				s.notify(notification);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		getLogger().info("Subscrion Linea Ventas", "Se ha cerrado la linea de ventas " + lv);
	}

	// ---------------------------------//
	// ---------Clases privadas---------//
	// ---------------------------------//
	/**
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

	/**
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
				Lot lot = null;
				try {
					lot = (Lot) msg.getContentObject();
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
				sender = msg.getSender();

				if (sellerAgents.containsKey(sender)) {
					// Seller is registred
					sellerAgents.get(sender).add(lot);

					// Seleccionamos aleatoriamente la linea de venta para ese lote
					Random rand = new Random();
					int randomLine = rand.nextInt(lines.size());
					int indice = (Integer) lineLots.keySet().toArray()[randomLine];
					lineLots.get(indice).add(lot);
					if (!lineasAbiertas.get(indice) && lines.get(indice).size() > 0 && lineLots.get(indice).size() > 1)
						iniciarLineaVenta(indice);
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

	/**
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

	/**
	 * 
	 * Clase privada que se encarga de recibir las peticiones por parte de los
	 * compradores de abrir una linea de credito. De esta comunicacion se encarga el
	 * RGC
	 *
	 */
	private class AperturaCreditoResponder extends AchieveREResponder {

		private static final long serialVersionUID = 1L;

		public AperturaCreditoResponder(Agent a, MessageTemplate mt) {
			super(a, mt);
		}

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
	}

	/**
	 * 
	 * Clase privada encargada de la comunicacion con el agente comprador cuando
	 * este solicita la retirada de los lotes comprados. El rol encargado de este
	 * compotamiento es RGC.
	 *
	 */
	private class RetiradaComprasResponder extends AchieveREResponder {

		private static final long serialVersionUID = 1L;

		public RetiradaComprasResponder(Agent a, MessageTemplate mt) {
			super(a, mt);
		}

		@Override
		protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response)
				throws FailureException {
			// Antes de enviar el mensaje se calcula la cantidad de dinero gastada por el
			// comprador y se decrementa esa cantidad de su linea de credito
			List<Lot> content = lotesComprador.get(request.getSender());
			float cantidadGastada = 0.0f;
			for (Lot l : content) {
				cantidadGastada += l.getPrecio();
			}
			ACLMessage informDone;
			if (lineasCredito.get(request.getSender()) == null) {
				informDone = request.createReply();
				informDone.setPerformative(ACLMessage.REFUSE);
				informDone.setContent("Debes abrir una linea de credito antes de poder retirar tus compras");
				return informDone;
			}
			lineasCredito.put(request.getSender(), lineasCredito.get(request.getSender()) - cantidadGastada);
			informDone = request.createReply();
			informDone.setPerformative(ACLMessage.INFORM);
			try {
				informDone.setContentObject((Serializable) content);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return informDone;
		}
	}

	/**
	 * 
	 * Clase privada encargada de la comunicación con el agente comprador para
	 * manejar la suscripcion a una subasta.
	 *
	 */
	private class SuscripcionLineaVentasResponder extends SubscriptionResponder {

		private static final long serialVersionUID = 1L;

		public SuscripcionLineaVentasResponder(Agent a, MessageTemplate mt) {
			super(a, mt);
		}

		@Override
		protected ACLMessage handleSubscription(ACLMessage subscription)
				throws NotUnderstoodException, RefuseException {
			ACLMessage response;

			if (subscriptionManager.register(createSubscription(subscription))) {
				response = subscription.createReply();
				response.setPerformative(ACLMessage.AGREE);

				int linea = Integer.parseInt(subscription.getContent());

				if (!lineasAbiertas.get(linea) && lines.get(linea).size() > 0 && lineLots.get(linea).size() > 0)
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
			// Tal vez habria que revisar el metodo deregister() para quitar ciertas
			// comprobaciones ya que estas se realizan ya aqui

			for (Integer linea : lines.keySet()) { // Como no se puede estar suscrito a mas de una linea de
													// venta a la vez en cuanto encontremos una suscripcion
													// a nombre del comprador que quiere cancelar la
													// suscripcion sabremos que esa es la unica que hay y
													// por lo tanto no es necesario revisar el resto de
													// suscripciones de las demas lineas de venta
				for (Subscription s : lines.get(linea)) {
					if (s.getMessage().getSender().equals(cancel.getSender())) {
						subscriptionManager.deregister(s);
						return super.handleCancel(cancel);
					}
				}
			}

			return super.handleCancel(cancel);
		}
	}

	/**
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

	/**
	 * 
	 * Clase privada encargada de manejar la comunicacion de la lonja con el agente
	 * comprador cuando se produce una subasta.
	 *
	 */
	private class SubastasLineaVentas extends Behaviour {

		private static final long serialVersionUID = 1L;
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;
		private int lineaVentas = 0;
		Lot lote = null;
		private boolean firstTime;
		private long t0;

		public SubastasLineaVentas(int lineaVentas) {
			this.lineaVentas = lineaVentas;
			this.firstTime = true;
		}

		@Override
		public void action() {
			if (firstTime) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				firstTime = false;
			}
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
					// Comprobamos que se ha recibido el mensaje y si se ha recibido, que tenga
					// suficiente dinero en la linea de credito
					if (reply != null && (lineasCredito.get(reply.getSender()) - lote.getPrecio()) >= 0) {
						AID sender = reply.getSender();
						// Borra el lote de la lina de ventas
						lineLots.get(lineaVentas).remove(lote);
						// Se añade el lote a la lista de lotes del comprador
						lotesComprador.get(sender).add(lote);
						// Se actualiza la linea de credito del comprador
						float lc = lineasCredito.get(sender);
						lineasCredito.put(sender, lc - lote.getPrecio());
						// Actualizamos nustros ingresos
						ingresos_actuales += lote.getPrecio();
						// Pagar al vendedor el precio de reserva
						pagarVendedor(lote);
						// Se responde con un inform al agente que ha realizado la puja
						ACLMessage response = reply.createReply();
						response.setContent("OK");
						response.setPerformative(ACLMessage.INFORM);
						getAgent().send(response);
						
						getLogger().info("Ingresos actuales", "La lonja tiene unos ingresos totales de " + ingresos_actuales + " euros.");
						
						// Volvemos al primer paso
						step = 0;

					} else {

						if (reply != null && lineasCredito.get(reply.getSender()) - lote.getPrecio() < 0) {
							ACLMessage response = reply.createReply();
							response.setContent("No tienes suficiente credito para hacer la puja");
							response.setPerformative(ACLMessage.REFUSE);
							getAgent().send(response);
						} else if (System.currentTimeMillis() - t0 >= lote.VENTANA_OPORTUNIDAD) {
							if (lote.getPrecio() == lote.getPrecioReserva()) {
								getLogger().info("Subasta lote: " + lote.getID(),
										"No se ha vendido el lote: " + lote.toString());
								// Borrar el lote de la linea de ventas
								lineLots.get(lineaVentas).remove(0);
								// Añadir el lote a la lista de reservas
								lotsReserva.add(lote);
								// Pagar al vendedor
								pagarVendedor(lote);
							} else {
								lote.setPrecio(lote.getPrecio() - lote.DECREM_PRECIO);
							}
							step = 0;
						} else {
							block(lote.VENTANA_OPORTUNIDAD);
						}

					}

					break;
				}
			} else {
				cerrarLinea(lineaVentas);
				getLogger().info("Ingresos actuales", "La lonja tiene unos ingresos totales de " + ingresos_actuales + " euros.");
				step = 2;
			}

		}

		@Override
		public boolean done() {
			return step == 2;
		}

	}
	// End of inner class SubastasLineaVentas

	/**
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
					// Acepta pago
					if (reply.getPerformative() == ACLMessage.INFORM) {
						ingresos_actuales -= dinero;
					} else { // Rechaza pago
						
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
