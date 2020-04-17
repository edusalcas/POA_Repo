package scenarios;

import java.util.List;


/**
 * Define los parámetros que conforman un escenario para su simulación desde un fichero YAML.
 * Ref: https://www.baeldung.com/java-snake-yaml
 * 
 *
 */
public class ScenarioConfig {
	private String name;
	private String description;
	private AgentRefConfig fishMarket;
	private List<AgentRefConfig> buyers;
	private List<AgentRefConfig> sellers;
	private List<AgentRefConfig> fishShips;
	
	@Override
	public String toString() {
		return "ScenarioConfig [name=" + name + ", description=" + description + ",\n"+
				"fishMarket=" + fishMarket + ",\n"+
				"buyers=" + buyers + ",\n"+
				"sellers=" + sellers + "\n"+
				"fishShips="+fishShips+"]";
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public AgentRefConfig getFishMarket() {
		return fishMarket;
	}
	public void setFishMarket(AgentRefConfig fishMarket) {
		this.fishMarket = fishMarket;
	}
	public List<AgentRefConfig> getBuyers() {
		return buyers;
	}
	public void setBuyers(List<AgentRefConfig> buyers) {
		this.buyers = buyers;
	}
	
	public List<AgentRefConfig> getSellers() {
		return sellers;
	}
	public void setSellers(List<AgentRefConfig> sellers) {
		this.sellers = sellers;
	}

	public List<AgentRefConfig> getFishShips() {
		return fishShips;
	}

	public void setFishShips(List<AgentRefConfig> fishShips) {
		this.fishShips = fishShips;
	}

}
