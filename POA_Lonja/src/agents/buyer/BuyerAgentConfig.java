package agents.buyer;

import java.util.List;

public class BuyerAgentConfig {
	private float budget;
	private List<Item> items;
	
	public List<Item> getItems() {
		return items;
	}

	public void setItems(List<Item> items) {
		this.items = items;
	}

	@Override
	public String toString() {
		return "BuyerAgentConfig [budget=" + budget + "]";
	}

	public float getBudget() {
		return budget;
	}

	public void setBudget(float budget) {
		this.budget = budget;
	}
}
