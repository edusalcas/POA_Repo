package agents.buyer;

import java.util.List;

public class BuyerAgentConfig {
	// ---------------------------------//
	// ------------Variables------------//
	// ---------------------------------//
	private float budget; // Cantidad de dinero disponible
	private List<Item> items; // Lotes que tiene el comprador

	// ---------------------------------//
	// ---------Override methods--------//
	// ---------------------------------//
	@Override
	public String toString() {
		return "BuyerAgentConfig [budget=" + budget + "]";
	}

	// ---------------------------------//
	// --------Getters & Setters--------//
	// ---------------------------------//
	public List<Item> getItems() {
		return items;
	}

	public void setItems(List<Item> items) {
		this.items = items;
	}

	public float getBudget() {
		return budget;
	}

	public void setBudget(float budget) {
		this.budget = budget;
	}
}
