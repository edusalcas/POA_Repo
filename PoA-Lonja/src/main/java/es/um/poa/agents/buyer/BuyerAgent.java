package es.um.poa.agents.buyer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.yaml.snakeyaml.Yaml;

import es.um.poa.agents.POAAgent;
import es.um.poa.agents.seller.Lot;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.AchieveREInitiator;
import jade.proto.SubscriptionInitiator;
import es.um.poa.utils.MessageCreator;

/**
 * Clase que representa al agente comprador
 * 
 * @author Eduardo Salmeron Castaño Francisco Hita Ruiz
 * 
 */
public class BuyerAgent extends POAAgent {

	// ---------------------------------//
	// ------------Variables------------//
	// ---------------------------------//
	private static final long serialVersionUID = 1L;
	private final int PROB_PUJAR = 50; // Probabilidad del comprador para pujar por un lote

	private float budget; // Cantidad de dinero disponible
	private List<Lot> lots; // Lotes que tiene el comprador
	private List<Item> listaCompra; // Lista de los elementos que quiere comprar

	private AID lonjaAgent; // Referencia al agente lonja
	private SubscriptionInitiator initiator; // Behaviour encargado de la suscrpción a la linea de venta

	// ---------------------------------//
	// ------------Funciones------------//
	// ---------------------------------//
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
			getLogger().info("ERROR", "Requiere fichero de cofiguracion.");
			doDelete();
		}
	}

	@Override
	public void takeDown() {
		// Cancelamos la suscripcion a la linea de venta
		initiator.cancel(lonjaAgent, true);

		// Printout a dismissal message
		System.out.println("Buyer-agent " + getAID().getName() + " terminating.");
		getLogger().info("", "Buyer-agent " + getAID().getName() + " terminating.");
		super.takeDown();
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

	/**
	 * Funcion encargada de inicializar el agente comprador
	 * 
	 * @param config parametros de configuracion
	 */
	private void init(BuyerAgentConfig config) {
		// Anunciamos que el agente ha sido creado
		System.out.println("Soy el agente comprador " + this.getName());

		// Introducimos los valores de configuracion en nuestro agente
		this.budget = config.getBudget();
		this.listaCompra = config.getItems();

		// Inicializamos atributos
		lots = new LinkedList<Lot>();
		
		// Buscamos al agente lonja
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("lonja");
		template.addServices(sd);
		try {
			DFAgentDescription[] result = DFService.search(this, template);
			while (result.length <= 0) {
				try {
					getLogger().info("Searching lonja", "Buscando al Agente Lonja");
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				result = DFService.search(this, template);
			}
			
			lonjaAgent = result[0].getName();

			
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		// Añadimos los Behaviours
		// (protocolo-admision-comprador)
		addBehaviour(new RequestAdmision());

	}

	// ---------------------------------//
	// -------Funciones privadas--------//
	// ---------------------------------//

	/**
	 * Funcion encargada de comprobar si un producto esta en la lista de deseados
	 * 
	 * @param producto nombre del producto
	 * @return Valor booleano, verdadero si es requerido, falso si no
	 */
	private boolean itemRequerido(String producto) {
		for (Item item : this.listaCompra) {
			if (item.getName().equals(producto))
				return true;
		}
		return false;
	}

	/**
	 * Funcion encargada de actualizar la lista de la compra, habiendo comprado un
	 * lote. Despues de actualizarla, se devuelve si la lista de la compra es vacia.
	 * 
	 * @param lote lote comprado
	 * @return Devuelve si la lista de la compra es vacia o no
	 */
	private boolean actualizarListaCompra(Lot lote) {

		Iterator<Item> it = listaCompra.iterator();
		while (it.hasNext()) {
			Item item = it.next();
			if (item.getName().equals(lote.getType())) {
				if ((item.getCantidad() - lote.getKg()) <= 0) {
					it.remove();
				} else
					item.setCantidad(item.getCantidad() - lote.getKg());
				break;
			}
		}
		return listaCompra.isEmpty();
	}

	// ---------------------------------//
	// ---------Clases privadas---------//
	// ---------------------------------//

	/**
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
						// (protocolo-apertura-crédito)
						myAgent.addBehaviour(
								new AperturaCredito(myAgent, MessageCreator.msgAperturaCredito(lonjaAgent, budget)));
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

	/**
	 * Clase privada encargada de la comunicacion con el RGC para abrir la linea de
	 * credito en la lonja.
	 */
	private class AperturaCredito extends AchieveREInitiator {

		boolean lineaCreditoCreada = false;
		private static final long serialVersionUID = 1L;

		// Constructor
		public AperturaCredito(Agent a, ACLMessage msg) {
			super(a, msg);
		}

		// Función encargada de manejar la llegada de un INFORM
		@Override
		protected void handleInform(ACLMessage inform) {
			lineaCreditoCreada = true;
			// (protocolo-subasta)
			initiator = new SuscripcionLineaVentas(myAgent, MessageCreator.msgSuscripcionLineaVentas(lonjaAgent, "1"));
			myAgent.addBehaviour(initiator);

			getLogger().info("Apertura credito", "Se ha abierto correctamente la linea de credito");
		}

		// Función encargada de manejar la llegada de un REFUSE
		@Override
		protected void handleRefuse(ACLMessage refuse) {
			getLogger().info("Apertura credito", "No se ha podido abrir una linea de credito");
		}

		// Función encargada de manejar la terminacion de la clase
		@Override
		protected boolean checkTermination(boolean currentDone, int currentResult) {
			if (super.checkTermination(currentDone, currentResult) && lineaCreditoCreada)
				return true;
			return false;
		}
	}
	// End of inner class AperturaCredito

	/**
	 * Clase privada encargada de la comunicacion con el RGC para retirar los lotes
	 * que el comprador ya ha comprado en las lineas de ventas.
	 */
	private class RetirarCompras extends AchieveREInitiator {

		private static final long serialVersionUID = 1L;

		// Constructor
		public RetirarCompras(Agent a, ACLMessage msg) {
			super(a, msg);
		}

		// Función encargada de manejar la llegada de un INFORM
		@Override
		protected void handleInform(ACLMessage inform) {
			try {
				// Obtenemos los lotes del mensaje y nos los añadimos
				@SuppressWarnings("unchecked")
				List<Lot> lotes = (List<Lot>) inform.getContentObject();
				lots.addAll(lotes);

				getLogger().info("Retirar compras", "Se han retirado correctamente " + lots.toString());

				getAgent().doDelete();
			} catch (UnreadableException e) {
				e.printStackTrace();
			}

		}

		// Función encargada de manejar la llegada de un REFUSE
		@Override
		protected void handleRefuse(ACLMessage refuse) {
			getLogger().info("Retirar compras", refuse.getContent());
			super.handleRefuse(refuse);
		}
	}
	// End of inner class RetirarCompras

	/**
	 * Clase privada encargada de la comunicacon con el RS para suscribirse a una
	 * linea de ventas.
	 */
	private class SuscripcionLineaVentas extends SubscriptionInitiator {

		private static final long serialVersionUID = 1L;

		// Constructor
		public SuscripcionLineaVentas(Agent a, ACLMessage msg) {
			super(a, msg);
		}

		// Función encargada de manejar la llegada de un AGREE
		@Override
		protected void handleAgree(ACLMessage agree) {
			super.handleAgree(agree);
			getLogger().info("Suscripcion linea ventas", "Suscripcion a linea de ventas realizada correctamente");
		}

		// Función encargada de manejar la llegada de un REFUSE
		@Override
		protected void handleRefuse(ACLMessage refuse) {
			super.handleRefuse(refuse);
			getLogger().info("Suscripcion linea ventas", refuse.getContent());
		}

		// Función encargada de manejar la llegada de un INFORM
		@Override
		protected void handleInform(ACLMessage inform) {
			try {
				// Recibimos el lote de la subasta
				Lot lote = (Lot) inform.getContentObject();

				// Comprobar si la subasta nos interesa
				Random rand = new Random();
				int valor = rand.nextInt(100);

				if (itemRequerido(lote.getType()) && valor >= PROB_PUJAR) {
					// Si nos interesa, llamar a un behaviour para realizar un FIPA-Request
					// aceptando la subasta
					ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
					request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
					request.addReceiver(lonjaAgent);
					request.setConversationId("realizar-puja-" + lote.getID());
					request.setContent(Integer.toString(lote.getID()));

					getAgent().addBehaviour(new RealizarPuja(getAgent(), request, lote));

					getLogger().info("Subasta lote: " + lote.getID(), "Se puja por el lote: " + lote.toString());
				}

				// Si no nos interesa no hacemos nada
			} catch (UnreadableException e) {
				getLogger().info("Subscribirse Linea Venta", "Hubo un error al recibir el lote");
				e.printStackTrace();
			}
		}

		// Función encargada de manejar la llegada de un FAILURE
		@Override
		protected void handleFailure(ACLMessage failure) {
			try {
				@SuppressWarnings("unchecked")
				ArrayList<Integer> lineasVenta = (ArrayList<Integer>) failure.getContentObject();
				if (!lineasVenta.isEmpty()) {
					Random rand = new Random();
					int indice = rand.nextInt(lineasVenta.size());
					int lv = lineasVenta.get(indice);
					getAgent().addBehaviour(new SuscripcionLineaVentas(getAgent(),
							MessageCreator.msgSuscripcionLineaVentas(lonjaAgent, Integer.toString(lv))));
				} else {
					getAgent()
							.addBehaviour(new RetirarCompras(getAgent(), MessageCreator.msgRetirarCompras(lonjaAgent)));
				}
				super.handleFailure(failure);
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
			// Creo que no tiene sentido manejar esta situacion

		}
	}
	// End of inner class SuscripcionLineaVentas

	/**
	 * Clase privada encargada de la comunicacion con el RS para realizar una puja
	 * por un lote. Mas especificamente espera a que el AL le responda si su puja ha
	 * sido aceptada
	 */
	private class RealizarPuja extends AchieveREInitiator {

		private static final long serialVersionUID = 1L;
		private Lot lote; // Lote del que se ha hecho la puja

		// Constructor
		public RealizarPuja(Agent subscriptionInitiator, ACLMessage msg, Lot lote) {
			super(subscriptionInitiator, msg);
			this.lote = lote;
		}

		// Función encargada de manejar la llegada de un INFORM
		@Override
		protected void handleInform(ACLMessage inform) {
			getLogger().info("Subasta lote: " + lote.getID(), "La lonja ha aceptado la puja por el paquete");

			// Restamos la cantidad de producto obtenida
			if (actualizarListaCompra(lote)) {
				// El AC ha completado su lista de la compra y se finaliza
				getLogger().info("Subasta lote: " + lote.getID(),
						"El agente " + getAgent().getLocalName() + " ha cumplido con su lista de la compra");

				// (protocolo-retirada-compras)
				myAgent.addBehaviour(new RetirarCompras(myAgent, MessageCreator.msgRetirarCompras(lonjaAgent)));
			}

		}

		// Función encargada de manejar la llegada de un REFUSE
		@Override
		protected void handleRefuse(ACLMessage refuse) {
			getLogger().info("Subasta lote: " + lote.getID(), refuse.getContent());
			super.handleRefuse(refuse);
		}

	}
	// End of inner class RealizarPuja
}
