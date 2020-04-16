package agents.buyer;

public class Item {
	// ---------------------------------//
	// ------------Variables------------//
	// ---------------------------------//
	private String name; // Nombre del item
	private float cantidad; // Cantidad que se necesita del item

	// ---------------------------------//
	// --------Getters & Setters--------//
	// ---------------------------------//
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public float getCantidad() {
		return cantidad;
	}

	public void setCantidad(float cantidad) {
		this.cantidad = cantidad;
	}

}
