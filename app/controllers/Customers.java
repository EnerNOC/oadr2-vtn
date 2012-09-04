package controllers;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import javax.persistence.Persistence;

import models.Program;
import models.VTN;
import play.data.Form;
import play.data.validation.ValidationError;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;

//export PATH=$PATH:/Users/jlajoie/Documents/play-2.0.1

public class Customers extends Controller {	

	public static Result index() {
		  return redirect(routes.Customers.customers());
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public static Result customers(){
		  List<VTN> customers = JPA.em().createQuery("FROM Customers").getResultList();
		  		  
		  class CustomerFormComparator implements Comparator<VTN>{
				public int compare(VTN userOne, VTN userTwo){
					return userOne.getVenID().compareTo(userTwo.getVenID());
				}
			}
		  
		  Collections.sort(customers, new CustomerFormComparator());
		  return ok(views.html.customers.render(customers));
	}
	
	public static Result blankCustomer(){
		return ok(views.html.newCustomer.render(form(VTN.class), makeProgramMap()));
	}
	
	@Transactional
	public static Result newCustomer(){
		  Form<VTN> filledForm = form(VTN.class).bindFromRequest();
		  if(filledForm.hasErrors()){
	    	  addFlashError(filledForm.errors());
			  return badRequest(views.html.newCustomer.render(filledForm, makeProgramMap()));
		  }
		  else{
			  VTN newCustomer = filledForm.get();
			  newCustomer.setProgramId(JPA.em().find(Program.class, Long.parseLong(newCustomer.getProgramId())).getProgramName());
			  JPA.em().persist(newCustomer);
			  flash("success", "Customer as been created");
		  }
		  return redirect(routes.Customers.customers());
	}
	
	//@Transactional
	//gives stupid "Try marking with @Transactional" error, yeah, it kinda was, kinda didnt work anyways
	@SuppressWarnings("unchecked")
    public static Map<String, String> makeProgramMap(){
	    //What the f this works but JPA.em() doesn't? ya okay sweet bro
	    List<Program> programList = Persistence.createEntityManagerFactory("Events").createEntityManager().createQuery("FROM Program").getResultList();
		//List<Program> programList = JPA.em().createQuery("FROM Program").getResultList();
		Map<String, String> programMap = new HashMap<String, String>();
		for(Program program : programList){
			programMap.put(program.getId() + "", program.getProgramName());
		}
		
		return programMap;
	}
	
	  @Transactional
	  public static Result deleteCustomer(Long id){
		  JPA.em().remove(JPA.em().find(VTN.class, id));
	      flash("success", "Customer has been deleted");
	      return redirect(routes.Customers.customers());
	  }
	
	  public static void addFlashError(Map<String, List<ValidationError>> errors){
		  for(String key : errors.keySet()){
			  List<ValidationError> currentError = errors.get(key);
			  for(ValidationError error : currentError){
				  flash(key, error.message());
			  }
		  }	  
	  }
}