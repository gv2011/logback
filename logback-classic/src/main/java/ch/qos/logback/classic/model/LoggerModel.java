package ch.qos.logback.classic.model;

import ch.qos.logback.core.model.Model;

public class LoggerModel extends Model {
 
    String name;
    String level;
    String additivity;
    

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getLevel() {
		return level;
	}
	public void setLevel(String level) {
		this.level = level;
	}
	
	public String getAdditivity() {
		return additivity;
	}

	public void setAdditivity(String additivity) {
		this.additivity = additivity;
	}

}
