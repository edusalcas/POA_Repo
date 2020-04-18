package agents.fishship;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.yaml.snakeyaml.Yaml;

import agents.POAAgent;
import agents.seller.Lot;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import utils.MessageCreator;

public class FishShipAgent extends POAAgent {

	// ---------------------------------//
	// ------------Variables------------//
	// ---------------------------------//
	private static final long serialVersionUID = 1L;

	List<Lot> mercancia; // Mercancía que tiene el barco, parte de la cual se le entregara al vendedor
	AID vendedor; // Vendedor, asociado al barco pesquero, que vendera la mercancia a la lonja

	// ---------------------------------//
	// ------------Funciones------------//
	// ---------------------------------//
	public void setup() {
		super.setup();
		Object[] args = getArguments();
		if (args != null && args.length == 1) {
			String configFile = (String) args[0];
			FishShipAgentConfig config = initAgentFromConfigFile(configFile);

			if (config != null) {
				init(config);
			}
		} else {
			this.getLogger().info("ERROR", "Requiere fichero de cofiguración.");
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

	private FishShipAgentConfig initAgentFromConfigFile(String fileName) {
		FishShipAgentConfig config = null;
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

	/*
	 * Funcion encargada de inicializar el agente lonja
	 */
	private void init(FishShipAgentConfig config) {
		// Anunciamos que el agente ha sido creado
		System.out.println("Soy el agente lonja " + this.getName());

		// Initialize variables
		mercancia = config.getLots();
		vendedor = new AID(config.getSeller(), AID.ISLOCALNAME);

		// Register the fish-ship service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("barco-pesquero");
		sd.setName("JADE-Lonja");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// Añadimos los Behaviours
		// (protocolo-entrega-mercancia)
		addBehaviour(
				new SuministroMercancia(this, MessageCreator.msgSuministroMercancia(vendedor, seleccionarMercancia())));

	}

	// ---------------------------------//
	// -------Funciones privadas--------//
	// ---------------------------------//
	/*
	 * Funcion que se encarga de seleccionar que mercancia de la que disponemos se
	 * le va a entregar al vendedor
	 */
	private Serializable seleccionarMercancia() {
		ArrayList<Lot> mercanciaSeleccionada = new ArrayList<Lot>();

		Random random = new Random();
		// Le damos al vendedor como minimo la mitad de los tipos de mercancia
		int nItems = random.nextInt(mercancia.size() / 2) + mercancia.size() / 2 + 1;
		for (int i = 0; i < nItems; i++) {
			// Seleccionamos aleatoriamente un tipo de mercancia
			int index = random.nextInt(mercancia.size());
			Lot lot = mercancia.remove(index);

			// De cada tipo de mercancia, le damos al vendedor entre un 20% y un 30%
			float porcentaje = (random.nextInt(30) + 20) / 100.0f;
			float roundOff = (float) (Math.round(lot.getKg() * porcentaje * 100.0) / 100.0);
			Lot lotEntrega = new Lot(lot.getType(), roundOff);
			
			// Añadimos el lote a la mercancia para el vendedor
			mercanciaSeleccionada.add(lotEntrega);

		}
		return (Serializable) mercanciaSeleccionada;
	}

	// ---------------------------------//
	// ---------Clases privadas---------//
	// ---------------------------------//

	/*
	 * Clase privada encargada de la comunicacion con el RV en la que se ofrece una
	 * parte de la mercancia, obtenida con un algoritmo, a un vendedor.
	 */
	private class SuministroMercancia extends AchieveREInitiator {

		private static final long serialVersionUID = 1L;

		// Constructor
		public SuministroMercancia(Agent a, ACLMessage msg) {
			super(a, msg);
		}

		// Función encargada de manejar la llegada de un INFORM
		@Override
		protected void handleInform(ACLMessage inform) {
			getLogger().info("Suministro Mercancia", "Se ha entregado la mercancia correctamente");
			super.handleInform(inform);
		}

		// Función encargada de manejar la llegada de un REFUSE
		@Override
		protected void handleRefuse(ACLMessage refuse) {
			getLogger().info("Suministro Mercancia", "Se ha rechazado la entrega de mercancia");
			super.handleRefuse(refuse);
		}

	}

}
