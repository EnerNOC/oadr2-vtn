package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/*
 * Not sure if this is the correct way to set up a relation by
 * making this new class with foreign keys and JPA annotation
 */
@Entity(name="ProjectEvent")
@Table(name="PROJECTEVENT")
public class ProjectEventRelation {
	
	private long eventId;
	private long projectId;
	@Id
	private long id;
	
	public ProjectEventRelation(){
		
	}
	
	public ProjectEventRelation(long eId, long pId){
		eventId = eId;
		projectId = pId;
	}
	
	public long getEventId() {
		return eventId;
	}
	@Column(name="EVENTID")
	public void setEventId(long eventId) {
		this.eventId = eventId;
	}
	
	@Column(name="PROJECTID")
	public long getProjectId() {
		return projectId;
	}
	public void setProjectId(long projectId) {
		this.projectId = projectId;
	}
	
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}

}
