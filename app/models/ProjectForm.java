package models;

import javax.persistence.*;

import play.data.validation.Constraints.Required;

@Entity
@Table(name="PROJECT")
//@Inheritance(strategy = InheritanceType.JOINED)
public class ProjectForm{

	@Column(name="projectname")
	@Required
	private String projectName;
	@Column(name="projecturi")
	@Required
	private String projectURI;
	@Id
	private long id;
	
	public ProjectForm(){
		
	}
	
	public ProjectForm(int projectId){
		this.setId(projectId);
		// also need to add setter for the other two fields from find statement
	}

	public String getProjectURI() {
		return projectURI;
	}

	public void setProjectURI(String projectURI) {
		this.projectURI = projectURI;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	
	
}
