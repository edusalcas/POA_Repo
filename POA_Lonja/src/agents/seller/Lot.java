package agents.seller;

public class Lot {
    private float kg;
    private String type;
     
    // Necesario para el yaml
	public Lot() {
		this.type = "";
		this.kg = 0;
	}
    
	public Lot(String type, float kg) {
		this.type = type;
		this.kg = kg;
	}

	@Override
	public String toString() {
		return "Lot [kg=" + kg + ", type=" + type + "]";
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
}
