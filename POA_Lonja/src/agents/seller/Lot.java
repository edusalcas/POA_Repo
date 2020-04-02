package agents.seller;

import java.util.concurrent.atomic.AtomicInteger;

import jade.util.leap.Serializable;

public class Lot implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private float kg;
	private String type;
	private float precioInicio;
	private float precioReserva;
	private float precio;
	private final int ID;
	
	private static final AtomicInteger count = new AtomicInteger(0);

	// Necesario para el yaml
	public Lot() {
		this.type = "";
		this.kg = 0;
		this.precioInicio = 10.0f;
		this.precioReserva = 5.0f;
		this.precio = 10.0f;
		this.ID = count.getAndIncrement();
	}

	public Lot(String type, float kg) {
		this.type = type;
		this.kg = kg;
		this.precioInicio = 10.0f;
		this.precioReserva = 5.0f;
		this.precio = 10.0f;
		this.ID = count.getAndIncrement();
	}

	@Override
	public String toString() {
		return "Lot [kg=" + kg + ", type=" + type + ", precio=" + precio + "]";
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

}
