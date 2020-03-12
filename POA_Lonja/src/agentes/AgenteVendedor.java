package agentes;

import java.util.Hashtable;

import gui.GuiComprador;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

@SuppressWarnings("serial")
public class AgenteVendedor extends Agent {

	// The GUI by means of which the user can add books in the catalogue
	private GuiComprador myGui;

	// Put agent initializations here
	@Override
	protected void setup() {
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
	}

	// Put agent clean-up operations here
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// Printout a dismissal message
		System.out.println("Seller-agent " + getAID().getName() + " terminating.");
	}

	public void nuevaMercancia(String title, int cantidad) {
		System.out.println("Nuevo paquete de mercancia con " + cantidad + "kg de " + title);
		
	}
}
