package es.um.poa.utils;

import java.io.IOException;
import java.io.Serializable;

import jade.core.AID;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import jade.proto.SubscriptionResponder;

/**
 * Clase que se encarga de crear el mensaje, o la plantilla, que se necesita
 * para crear una clase JADE (AchieveREInitiator, SubscriptionInitiator...)
 * 
 * @author Eduardo Salmeron Castaño Francisco Hita Ruiz
 */
public class MessageCreator {

	// ---------------------------------//
	// ------------Comprador------------//
	// ---------------------------------//

	/**
	 * Crea el mensaje del comprador para el protocolo apertura credito
	 * 
	 * @param lonjaAgent el AID de la lonja
	 * @param budget     cantidad con la que se abre la linea de credito
	 * @return El mensaje
	 */
	public static ACLMessage msgAperturaCredito(AID lonjaAgent, float budget) {
		ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		request.addReceiver(lonjaAgent);
		request.setContent(Float.toString(budget));
		request.setConversationId("apertura-credito");
		return request;
	}

	/**
	 * Crea el mensaje del comprador para el protocolo retirada compras
	 * 
	 * @param lonjaAgent AID de la lonja
	 * @return El mensaje creado
	 */
	public static ACLMessage msgRetirarCompras(AID lonjaAgent) {
		ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		request.addReceiver(lonjaAgent);
		request.setConversationId("retirar-compras");
		return request;
	}

	/**
	 * Crea el mensaje del comprador para el protocolo suscripcion linea ventas
	 * 
	 * @param lonjaAgent AID de la lonja
	 * @param lineaVenta numero de la linea de ventas a la que suscribirse
	 * @return El mensaje creado
	 */
	public static ACLMessage msgSuscripcionLineaVentas(AID lonjaAgent, String lineaVenta) {
		ACLMessage request = new ACLMessage(ACLMessage.SUBSCRIBE);
		request.setProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
		request.addReceiver(lonjaAgent);
		request.setConversationId("subs-linea_venta");
		request.setContent(lineaVenta);
		return request;
	}

	// ---------------------------------//
	// ---------------Lonja-------------//
	// ---------------------------------//

	/**
	 * Crea el mensaje de la lonja para el protocolo apertura de credito
	 * 
	 * @return El mensaje creado
	 */
	public static MessageTemplate msgAperturaCreditoResponder() {
		MessageTemplate mt = MessageTemplate.and(
				AchieveREResponder.createMessageTemplate(FIPANames.InteractionProtocol.FIPA_REQUEST),
				MessageTemplate.MatchConversationId("apertura-credito"));
		return mt;
	}

	/**
	 * Crea el mensaje de la lonja para el protocolo retirada compras
	 * 
	 * @return El mensaje creado
	 */
	public static MessageTemplate msgRetiradaComprasResponder() {
		MessageTemplate mt = MessageTemplate.and(
				AchieveREResponder.createMessageTemplate(FIPANames.InteractionProtocol.FIPA_REQUEST),
				MessageTemplate.MatchConversationId("retirar-compras"));
		return mt;
	}

	/**
	 * Crea el mensaje de la lonja para el protocolo suscripcion linea de ventas
	 * 
	 * @return El mensaje creado
	 */
	public static MessageTemplate msgSuscripcionLineaVentasResponder() {
		MessageTemplate mt = MessageTemplate.and(SubscriptionResponder.createMessageTemplate(ACLMessage.SUBSCRIBE),
				MessageTemplate.MatchConversationId("subs-linea_venta"));
		return mt;
	}

	// ---------------------------------//
	// ------------Vendedor-------------//
	// ---------------------------------//

	/**
	 * Crea el mensaje del vendedor para el protocolo suministro de mercancias
	 * 
	 * @param name nombre del agente vendedor
	 * @return El mensaje creado
	 */
	public static MessageTemplate msgSuministroMercanciaResponder(String name) {
		MessageTemplate mt = MessageTemplate.and(
				AchieveREResponder.createMessageTemplate(FIPANames.InteractionProtocol.FIPA_REQUEST),
				MessageTemplate.MatchConversationId("entrega-mercancia-" + name));
		return mt;
	}

	// ---------------------------------//
	// ---------Barco pesquero----------//
	// ---------------------------------//
	/**
	 * Crea el mensaje del barco pesquero para el protocolo suministro de mercancias
	 * 
	 * @param vendedor  vendedor asociado al barco
	 * @param mercancia mercancia que se le va a entregar al vendedor
	 * @return El mensaje creado
	 */
	public static ACLMessage msgSuministroMercancia(AID vendedor, Serializable mercancia) {
		ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		try {
			request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
			request.addReceiver(vendedor);
			request.setContentObject(mercancia);
			request.setConversationId("entrega-mercancia-" + vendedor.getLocalName());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return request;
	}

}
