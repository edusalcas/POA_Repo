package poa;

import jade.core.Agent;

public class Agente1 extends Agent {

	@Override
	protected void setup() {
		System.out.println("Hola mundo soy el agente: " + this.getName());
	}

	protected void takeDown() {
		System.out.println("Adios mundo soy el agente: " + this.getName());
	}
}
