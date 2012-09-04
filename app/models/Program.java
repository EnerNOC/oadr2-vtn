package models;

import javax.persistence.*;
import play.data.validation.Constraints.Required;

@Entity(name="Program")
@Table(name="PROJECT")
//@Inheritance(strategy = InheritanceType.JOINED)
public class Program{
	
	@Required(message = "Must entre a valid Program Name")
	private String programName;
	@Required(message = "Must enter a valid Program URI")
	private String programURI;

	@Id private long id;
	
	public Program(){
		
	}
	
	public Program(int programId){
		this.setId(programId);
		// also need to add setter for the other two fields from find statement
	}

	@Column(name="PROJECTURI")
	public String getProgramURI() {
		return programURI;
	}

	public void setProgramURI(String programURI) {
		this.programURI = programURI;
	}

	@Column(name="PROJECTNAME")
	public String getProgramName() {
		return programName;
	}

	public void setProgramName(String programName) {
		this.programName = programName;
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
