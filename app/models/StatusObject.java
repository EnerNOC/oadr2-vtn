package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity(name = "StatusObject")
@Table(name = "STATUSOBJECT")
public class StatusObject {
	
	@Id 
	@Column(name = "ID")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	
	@Column(name = "EVENTID")
	private String eventId;
	
	@Column(name = "CUSTOMERID")
	private String customerId;
	
	@Column(name = "OPTSTATUS")
	private String optStatus;
	
	@Column(name = "PROGRAM")
	private String program;
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getEventId() {
		return eventId;
	}
	public void setEventId(String eventId) {
		this.eventId = eventId;
	}
	public String getCustomerId() {
		return customerId;
	}
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}
	public String getOptStatus() {
		return optStatus;
	}
	public void setOptStatus(String optStatus) {
		this.optStatus = optStatus;
	}
	public String getProgram() {
		return program;
	}
	public void setProgram(String program) {
		this.program = program;
	}
	
	
}
