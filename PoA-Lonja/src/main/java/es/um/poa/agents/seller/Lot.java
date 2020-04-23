package es.um.poa.agents.seller;

import java.util.concurrent.atomic.AtomicInteger;

import jade.util.leap.Serializable;

public class Lot implements Serializable {

	// ---------------------------------//
	// ------------Variables------------//
	// ---------------------------------//

	private static final long serialVersionUID = 1L;
	private int ID; // Identificador unico de cada lote
	private static final AtomicInteger count = new AtomicInteger(0); // Variable que se autoincrementa para que el id
																		// sea unico

	private float kg; // Cantidad del lote
	private String type; // Tipo de producto del lote
	private float precioInicio; // Precio con el que empieza el lote
	private float precioReserva; // Precio hasta el que puede bajar el lote
	private float precio; // Precio actual del lote

	// ---------------------------------//
	// ----------Constructores----------//
	// ---------------------------------//
	
	// Constructor vacio necesario para el yaml
	public Lot() {
		this.type = "";
		this.kg = 0;
		this.precioInicio = 10.0f;
		this.precioReserva = 5.0f;
		this.precio = 10.0f;
		this.ID = 0;
	}

	// Constructor para los lotes que se introduzcan por gui (tendran un precio fijo)
	public Lot(String type, float kg) {
		this.type = type;
		this.kg = kg;
		this.precioInicio = 10.0f;
		this.precioReserva = 5.0f;
		this.precio = 10.0f;
		this.ID = 0;
	}

	// ---------------------------------//
	// ------Metodos sobreescritos------//
	// ---------------------------------//
	
	@Override
	public String toString() {
		return "Lot [kg=" + kg + ", type=" + type + ", precio=" + precio + " ID=" + ID + "]";
	}

	

	// ---------------------------------//
	// --------Getters & Setters--------//
	// ---------------------------------//
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ID;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Lot other = (Lot) obj;
		if (ID != other.ID)
			return false;
		return true;
	}

	public float getKg() {
		return kg;
	}

	public void setKg(float kg) {
		this.kg = kg;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public float getPrecioInicio() {
		return precioInicio;
	}

	public void setPrecioInicio(float precioInicio) {
		this.precioInicio = precioInicio;
	}

	public float getPrecioReserva() {
		return precioReserva;
	}

	public void setPrecioReserva(float precioReserva) {
		this.precioReserva = precioReserva;
	}

	public float getPrecio() {
		return precio;
	}

	public void setPrecio(float precio) {
		this.precio = precio;
	}

	public int getID() {
		return ID;
	}

	public void setID(int iD) {
		ID = iD;
	}
	
	

}
