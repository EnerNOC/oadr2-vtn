package models;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlAttribute;

import play.data.validation.Constraints.Required;

@Entity(name="Project")
@Table(name="PROJECT")
//@Inheritance(strategy = InheritanceType.JOINED)
public class ProjectForm{
	
	@Required
	private String projectName;
	@Required
	private String projectURI;

	@Id private long id;
	
	public ProjectForm(){
		
	}
	
	public ProjectForm(int projectId){
		this.setId(projectId);
		// also need to add setter for the other two fields from find statement
	}

	@Column(name="PROJECTURI")
	public String getProjectURI() {
		return projectURI;
	}

	public void setProjectURI(String projectURI) {
		this.projectURI = projectURI;
	}

	@Column(name="PROJECTNAME")
	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
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
