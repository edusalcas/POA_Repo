package agents.seller;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.yaml.snakeyaml.Yaml;

import agents.POAAgent;
import agents.AgenteVendedor.DepositoDeCaptura;
import agents.AgenteVendedor.RequestRegistro;
import gui.GuiComprador;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class SellerAgent extends POAAgent  {
		
	private static final long serialVersionUID = 1L;
	// The GUI by means of which the user can add books in the catalogue
	private GuiComprador myGui;
	
	public void setup() {
		super.setup();
		
		Object[] args = getArguments();
		if (args != null && args.length == 1) {
			String configFile = (String) args[0];
			SellerAgentConfig config = initAgentFromConfigFile(configFile);
			
			if(config != null) {
				System.out.println("Soy el agente vendedor " + this.getName());

				// Create and show the GUI
				myGui = new GuiComprador(this);
				myGui.showGui();

				// Register the book-selling service in the yellow pages
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

				addBehaviour(new RequestRegistro());
			} else {
				doDelete();
			}
		} else {
			getLogger().info("ERROR", "Requiere fichero de cofiguración.");
			doDelete();
		}
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
	
	public void nuevaMercancia(String title, int cantidad) {
		System.out.println("Nuevo paquete de mercancia con " + cantidad + "kg de " + title);
		
		addBehaviour(new DepositoDeCaptura(title, cantidad));
	}
	
	private class DepositoDeCaptura extends Behaviour {

		private MessageTemplate mt; // The template to receive replies
		private int step = 0;

		private String title;
		private int cantidad;
		
		public DepositoDeCaptura(String title, int cantidad) {
			this.title = title;
			this.cantidad = cantidad;
		}
		
		@Override
		public void action() {
			switch (step) {
			case 0:
				// Enviar request al agente lonja con rol RAV
				ACLMessage req = new ACLMessage(ACLMessage.REQUEST);

				req.addReceiver(lonjaAgent);
				req.setConversationId("deposito-captura");
				req.setContent(title + "," + cantidad); 
				req.setReplyWith("dep" + System.currentTimeMillis()); // Unique value

				myAgent.send(req);
				// Prepare the template to get proposals
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
						System.out.println("Deposito de capturas a vender exitoso");
					} else {
						// Fallo en el deposito de capturas
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
	// End of inner class Deposito de Capturas
}
