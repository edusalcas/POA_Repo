package es.um.poa.agents.fishship;

import java.util.List;

import es.um.poa.agents.seller.Lot;

public class FishShipAgentConfig{

	List<Lot> lots;
	String seller;
	
	@Override
	public String toString() {
		return "FishShipAgentConfig [lots=" + lots + "]";
	}

	public List<Lot> getLots() {
		return lots;
	}

	public void setLots(List<Lot> lots) {
		this.lots = lots;
	}

	public String getSeller() {
		return seller;
	}

	public void setSeller(String seller) {
		this.seller = seller;
	}
	
	
}
