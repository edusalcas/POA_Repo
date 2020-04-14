package agents.buyer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.yaml.snakeyaml.Yaml;

import agents.POAAgent;
import agents.seller.Lot;
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

public class BuyerAgent extends POAAgent{

	private static final long serialVersionUID = 1L;

	private AID lonjaAgent = new AID("lonja", AID.ISLOCALNAME);

	private float budget;

	private boolean lineaCredito;

	private List<Lot> lots;
	
	private boolean registered;
	
	private final int PROB_PUJAR = 92;
	
	
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
		registered = false;
		
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
		
			addBehaviour(new AchieveREInitiator(this, request) {
				private static final long serialVersionUID = 1L;

				@Override
				protected void handleInform(ACLMessage inform) {
					getLogger().info("Apertura cr�dito", "Se ha abierto correctamente la l�nea de cr�dito");
					lineaCredito = true;
				}
				
				@Override
				protected void handleRefuse(ACLMessage refuse) {
					getLogger().info("Apertura cr�dito", "No se ha podido abrir una l�nea de cr�dito");
				}
				
				@Override
				protected boolean checkTermination(boolean currentDone, int currentResult) {
					if(super.checkTermination(currentDone, currentResult) && lineaCredito) return true;
					return false;
				}

			});
		

		// Retirada Compras
		request = new ACLMessage(ACLMessage.REQUEST);
		request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		
		request.addReceiver(lonjaAgent);
		request.setConversationId("retirar-compras");
		addBehaviour(new AchieveREInitiator(this, request) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected void handleInform(ACLMessage inform) {
				try {
					@SuppressWarnings("unchecked")
					List<Lot> lotes = (List<Lot>) inform.getContentObject();
					lots = lotes;
					getLogger().info("Retirar compras", "Se han retirado correctamente " + lots.toString());
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
				
			}
			
		});
		
		//Subscribirse a Linea-Venta
		request = new ACLMessage(ACLMessage.SUBSCRIBE);
		request.setProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
		request.addReceiver(lonjaAgent);
		request.setConversationId("subs-linea_venta");
		request.setContent("1");
		addBehaviour(new SubscriptionInitiator(this, request) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void handleAgree(ACLMessage agree) {
				super.handleAgree(agree);
				getLogger().info("Suscripcion linea ventas", "Suscripcion a linea de ventas realizada correctamente");
			}
			
			@Override
			protected void handleRefuse(ACLMessage refuse) {
				super.handleRefuse(refuse);
				getLogger().info("Suscripcion linea ventas", refuse.getContent());
			}
			
			// Hemos recibo una subasta
			@Override
			protected void handleInform(ACLMessage inform) {
				try {
					// Recibimos el lote de la subasta
					Lot lote = (Lot) inform.getContentObject();
					
					// Comprobar si la subasta nos interesa
					Random rand = new Random();
					int valor = rand.nextInt(100);
					
					if (valor >= PROB_PUJAR) {
						// Si nos interesa, llamar a un behaviour para realizar un FIPA-Request aceptando la subasta
						ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
						request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
						request.addReceiver(lonjaAgent);
						request.setConversationId("realizar-puja-" + lote.getID());
						request.setContent(Integer.toString(lote.getID()));
						getAgent().addBehaviour(new RealizarPuja(getAgent(), request, lote));
						getLogger().info("Subasta lote: "+lote.getID(), "Se puja por el lote: " + lote.toString());
					}
					
					// Si no nos interesa no hacemos nada
				} catch (UnreadableException e) {
					getLogger().info("Subscribirse Linea Venta", "Hubo un error al recibir el lote");
					e.printStackTrace();
				}
			}
			
			@Override
			protected void handleFailure(ACLMessage failure) {
				//Creo que no tiene sentido manejar esta situacion
				getLogger().info("Linea de venta cerrada", "LV cerrada para el comprador " + myAgent.getLocalName());
				super.handleFailure(failure);
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
						registered = true;
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
	
	private class RealizarPuja extends AchieveREInitiator{

		private static final long serialVersionUID = 1L;
		private Lot lote;
		
		public RealizarPuja(Agent subscriptionInitiator, ACLMessage msg, Lot lote) {
			super(subscriptionInitiator, msg);
			this.lote = lote;
		}

		@Override
		protected void handleInform(ACLMessage inform) {
			getLogger().info("Subasta lote: "+lote.getID(), "La lonja ha aceptado la puja por el paquete");

		}
		
	}

	

}
