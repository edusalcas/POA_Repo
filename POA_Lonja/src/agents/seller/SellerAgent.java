package agents.seller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import agents.POAAgent;

import gui.GuiVendedor;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.AchieveREResponder;
import utils.MessageCreator;

public class SellerAgent extends POAAgent {

	// ---------------------------------//
	// ------------Variables------------//
	// ---------------------------------//
	private static final long serialVersionUID = 1L;
	private final int PROB_ACEPTAR_PAGO = 10; // Probabilidad de aceptar un pago de la lonja por un lote vendido

	private GuiVendedor myGui; // Interfaz donde se ingresan mas lotes
	private AID lonjaAgent = new AID("lonja", AID.ISLOCALNAME); // Direccion de la lonja

	private ArrayList<Lot> lots; // Lotes de los que dispone el vendedor
	private float dinero; // Dinero ganado por el vendedor

	// ---------------------------------//
	// ------------Funciones------------//
	// ---------------------------------//

	public void setup() {
		super.setup();
		// No necesitamos cargar archivo de configuracion ya que los lotes nos los
		// proporcionara el barco pesquero
		init();
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
		System.out.println("Seller-agent " + getAID().getName() + " terminating.");
		super.takeDown();
	}

	/*
	 * Funcion encargada de inicializar el agente vendedor
	 */
	private void init() {
		// Anunciamos que el agente ha sido creado
		System.out.println("Soy el agente vendedor " + this.getName());

		// Create and show the GUI
		myGui = new GuiVendedor(this);
		myGui.showGui();

		// Inicializar variables
		lots = new ArrayList<Lot>();
		dinero = 0.0f;

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

		// Añadimos los Behaviours
		// (protocolo-registro-vendedor) El RAV recibe la petición de registro del RV
		addBehaviour(new RequestRegistro());

		// (protocolo-deposito) El RRV recibe la petición de hacer un deposito de
		// capturas del RV
		addBehaviour(new DepositoDeCaptura());

		// (protocolo-cobro)
		addBehaviour(new RecibirCobro());

		// (protocolo-suministro-mercancia)
		addBehaviour(new SuministroMercanciaResponder(this,
				MessageCreator.msgSuministroMercanciaResponder(getAID().getLocalName())));
	}

	// ---------------------------------//
	// -------Funciones privadas--------//
	// ---------------------------------//

	/*
	 * Funcion encargada de añadir un paquete mediante la GUI
	 */
	public void nuevaMercancia(String type, float kg) {
		System.out.println("Nuevo paquete de mercancia con " + kg + "kg de " + type);
		
		Lot lot = new Lot(type, kg);
		lots.add(lot);
	}

	// ---------------------------------//
	// ---------Clases privadas---------//
	// ---------------------------------//

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

	/*
	 * Clase privada encargada de la comunicacion con RRV para hacer un deposito de un 
	 * lote en la lonja
	 */
	private class DepositoDeCaptura extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;

		@Override
		public void action() {
			// Si hay lots que depositar
			if (!lots.isEmpty()) {
				switch (step) {
				case 0:
					// Obtenemos el primer lot
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
	// End of inner class DepositoDeCaptura

	/*
	 * Clase privada encargada de la comuniacion con el RGV para recibir el pago 
	 * por una captura que ha sido vendida en la lonja
	 */
	private class RecibirCobro extends CyclicBehaviour {

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
				Random rand = new Random();
				int randonResponse = rand.nextInt(100);
				if (randonResponse >= PROB_ACEPTAR_PAGO) {
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
	// End of inner class RecieveCobro

	/*
	 * Clase encargada de la comunicación con el RB, para recibir las mercancias que
	 * este le entregue
	 */
	private class SuministroMercanciaResponder extends AchieveREResponder {

		private static final long serialVersionUID = 1L;

		// Constructor
		public SuministroMercanciaResponder(Agent a, MessageTemplate mt) {
			super(a, mt);
		}

		// Función encargada de manejar la llegada de un REQUEST
		@Override
		protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {
			// Añadir la mercancia
			try {
				@SuppressWarnings("unchecked")
				List<Lot> mercancia = (List<Lot>) request.getContentObject();
				lots.addAll(mercancia);
				getLogger().info("Suministro Mercancia", "Añadidos los lotes: " + mercancia);
			} catch (UnreadableException e) {
				e.printStackTrace();
			}

			// Crear un INFORM como confirmacion de la mercancia
			ACLMessage informDone = request.createReply();
			informDone.setPerformative(ACLMessage.INFORM);

			return super.handleRequest(informDone);
		}

	}
	// End of inner class SuministroMercanciaResponder
}
