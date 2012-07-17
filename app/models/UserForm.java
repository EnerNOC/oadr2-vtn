package models;

import javax.persistence.*;

import play.data.validation.Constraints.Required;

@Entity(name="Users")
@Table(name="USERS")
//@Inheritance(strategy = InheritanceType.JOINED)
public class UserForm{
	
	@Required
	private String userName;
	@Required
	private String projectId;

	@Id private long id;
	
	public UserForm(){
		
	}
	
	public UserForm(int projectId){
		this.setId(projectId);
		// also need to add setter for the other two fields from find statement
	}

	public String getUserName() {
		return userName;
	}

	@Column(name = "USERNAME")
	public void setUserName(String userName) {
		this.userName = userName;
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

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
	public void setId(long id) {
		this.id = id;
	}

}
