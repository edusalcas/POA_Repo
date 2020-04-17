package utils;

import java.io.IOException;
import java.io.Serializable;

import jade.core.AID;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import jade.proto.SubscriptionResponder;


/*
 * Clase que se encarga de crear el mensaje, o la plantilla, que se necesita 
 * para crear una clase JADE (AchieveREInitiator, SubscriptionInitiator...)
 */
public class MessageCreator {

	// ---------------------------------//
	// ------------Comprador------------//
	// ---------------------------------//

	public static ACLMessage msgAperturaCredito(AID lonjaAgent, float budget) {
		ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		request.addReceiver(lonjaAgent);
		request.setContent(Float.toString(budget));
		request.setConversationId("apertura-credito");
		return request;
	}

	public static ACLMessage msgRetirarCompras(AID lonjaAgent) {
		ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		request.addReceiver(lonjaAgent);
		request.setConversationId("retirar-compras");
		return request;
	}

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

	public static MessageTemplate msgAperturaCreditoResponder() {
		MessageTemplate mt = MessageTemplate.and(
				AchieveREResponder.createMessageTemplate(FIPANames.InteractionProtocol.FIPA_REQUEST),
				MessageTemplate.MatchConversationId("apertura-credito"));
		return mt;
	}

	public static MessageTemplate msgRetiradaComprasResponder() {
		MessageTemplate mt = MessageTemplate.and(
				AchieveREResponder.createMessageTemplate(FIPANames.InteractionProtocol.FIPA_REQUEST),
				MessageTemplate.MatchConversationId("retirar-compras"));
		return mt;
	}

	public static MessageTemplate msgSuscripcionLineaVentasResponder() {
		MessageTemplate mt = MessageTemplate.and(
				SubscriptionResponder.createMessageTemplate(ACLMessage.SUBSCRIBE),
				MessageTemplate.MatchConversationId("subs-linea_venta"));
		return mt;
	}

	// ---------------------------------//
	// ------------Vendedor-------------//
	// ---------------------------------//

	public static MessageTemplate msgSuministroMercanciaResponder(String name) {
		MessageTemplate mt = MessageTemplate.and(
				AchieveREResponder.createMessageTemplate(FIPANames.InteractionProtocol.FIPA_REQUEST),
				MessageTemplate.MatchConversationId("entrega-mercancia-" + name));
		return mt;
	}
	
	// ---------------------------------//
	// ---------Barco pesquero----------//
	// ---------------------------------//
	
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
