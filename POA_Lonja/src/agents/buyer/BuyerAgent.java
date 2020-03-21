package agents.buyer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

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
import jade.proto.AchieveREInitiator;

public class BuyerAgent extends POAAgent {
		

	private static final long serialVersionUID = 1L;
	
	private AID lonjaAgent = new AID("lonja", AID.ISLOCALNAME);
	
	private float budget;
	
	private boolean lineaCredito;
	
	private ArrayList<Lot> lots;

	public void setup() {
		super.setup();
		
		Object[] args = getArguments();
		if (args != null && args.length == 1) {
			String configFile = (String) args[0];
			BuyerAgentConfig config = initAgentFromConfigFile(configFile);
			
			if(config != null) {
				init(config);
			} else {
				doDelete();
			}
		} else {
			getLogger().info("ERROR", "Requiere fichero de cofiguraciÃ³n.");
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
		System.out.println("Soy el agente comprador "+this.getName());
		
		// Registramos el agente comprador en las páginas amarillas
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
		
		//Introducimos los valores de configuración en nuestro agente
		this.budget = config.getBudget();
		
		//Añadimos los Behaviours
		
		
		//Apertura de crédito
		ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		request.addReceiver(lonjaAgent);
		request.setContent(Float.toString(budget));
		request.setConversationId("apertura-credito");
		while(lineaCredito != true) {
			addBehaviour(new AchieveREInitiator(this, request) {
				private String estado;
				@Override
				protected void handleInform(ACLMessage inform) {
					if(inform.getContent() == "OK") {
						lineaCredito = true;
					}
				}
				
				
			});
		}
		
		//Retirada Compras
		request = new ACLMessage(ACLMessage.REQUEST);
		request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		request.addReceiver(lonjaAgent);
		request.setConversationId("retirar-compras");
		addBehaviour(new AchieveREInitiator(this, request) {
			@Override
			protected void handleInform(ACLMessage inform) {
				//TODO
				if(inform.getContent() == )
			}
		});
		
	}
	
	
}
