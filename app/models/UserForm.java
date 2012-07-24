package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.*;

import play.data.validation.Constraints.Required;

@Entity(name="Users")
@Table(name="USERS")
//@Inheritance(strategy = InheritanceType.JOINED)
public class UserForm{
	
	public String programName;
	
	@Required
	@Column(name = "VENID")
	private String venID;
	
	@Required
	@Column(name = "PROJECTID")
	private String projectId;
	
	@ElementCollection
	@Column(name = "VENS")
	private List <String> vens = new ArrayList<String>();
	//private List<VenForm> vens;
	
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
		
	public UserForm(){
		
	}
	
	public UserForm(int projectId){
		this.setId(projectId);
		// also need to add setter for the other two fields from find statement
	}

	public String getVenID() {
		return venID;
	}

	public void setVenID(String userName) {
		this.venID = userName;
	}

	public String getProjectId() {
		return projectId;
	}

	@Column(name = "PROJECTID")
	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public List <String> getVens() {
		return vens;
	}

	public void setVens(List <String> vens) {
		this.vens = vens;
	}

}
