package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

import play.data.validation.Constraints.Required;

@Entity(name="Customers")
@Table(name="CUSTOMERS")
//@Inheritance(strategy = InheritanceType.JOINED)
public class CustomerForm{
	
	public String programName;
		
	@Required(message = "Must enter a valid VEN ID")
	@Column(name = "VENID")
	private String venID;
	
	@Required(message = "Must select a Program")
	@Column(name = "PROGRAMID")
	private String programId;
	
	@Column(name = "CUSTOMERNAME")
	private String customerName;
	
	@Required(message = "Must enter a Client URI")
	@Column(name = "CLIENTURI")
	private String clientURI;
	
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
		
	public CustomerForm(){
		
	}
	
	public CustomerForm(int programId){
		this.setId(programId);
		// also need to add setter for the other two fields from find statement
	}

	public String getVenID() {
		return venID;
	}

	public void setVenID(String userName) {
		this.venID = userName;
	}

	public String getProgramId() {
		return programId;
	}

	@Column(name = "PROJECTID")
	public void setProgramId(String programId) {
		this.programId = programId;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getClientURI() {
        return clientURI;
    }

    public void setClientURI(String clientURI) {
        this.clientURI = clientURI;
    }

}
