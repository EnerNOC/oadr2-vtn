package controllers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.enernoc.open.oadr2.model.EiEvent;

import models.ProgramForm;
import models.StatusObject;
import models.CustomerForm;
import play.Logger;
import play.data.Form;
import play.data.validation.ValidationError;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;

//export PATH=$PATH:/Users/jlajoie/Documents/play-2.0.1

public class Customers extends Controller {
	static EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("Events");
	static EntityManager entityManager = entityManagerFactory.createEntityManager();
	

	public static Result index() {
		  return redirect(routes.Customers.customers());
	}

	@SuppressWarnings("unchecked")
	public static Result customers(){
		  createNewEm();
		  List<CustomerForm> customers = entityManager.createQuery("FROM Customers").getResultList();
		  		  
		  class CustomerFormComparator implements Comparator<CustomerForm>{
				public int compare(CustomerForm userOne, CustomerForm userTwo){
					return userOne.getVenID().compareTo(userTwo.getVenID());
				}
			}
		  
		  Collections.sort(customers, new CustomerFormComparator());
		  return ok(views.html.customers.render(customers));
	}
	
	public static Result blankCustomer(){
		return ok(views.html.newCustomer.render(form(CustomerForm.class), makeProgramMap()));
	}
	
	@Transactional
	public static Result newCustomer(){
		  Form<CustomerForm> filledForm = form(CustomerForm.class).bindFromRequest();
		  if(filledForm.hasErrors()){
	    	  addFlashError(filledForm.errors());
			  return badRequest(views.html.newCustomer.render(filledForm, makeProgramMap()));
		  }
		  else{
			  createNewEm();
			  CustomerForm newCustomer = filledForm.get();
			  newCustomer.setProgramId(entityManager.find(ProgramForm.class, Long.parseLong(newCustomer.getProgramId())).getProgramName());
			  entityManager.persist(newCustomer);
			  entityManager.getTransaction().commit();
			  flash("success", "Customer as been created");
		  }
		  return redirect(routes.Customers.customers());
	}
	
	@SuppressWarnings("unchecked")
	@Transactional(readOnly=true)
	public static Map<String, String> makeProgramMap(){
		createNewEm();
		List<ProgramForm> programList = entityManager.createQuery("FROM Program").getResultList();
		Map<String, String> programMap = new HashMap<String, String>();
		for(ProgramForm program : programList){
			programMap.put(program.getId() + "", program.getProgramName());
		}
		
		return programMap;
	}
	
	  @Transactional
	  //Deletes an event based on the id
	  public static Result deleteCustomer(Long id){
		  createNewEm();
		  entityManager.remove(entityManager.find(CustomerForm.class, id));
		  entityManager.getTransaction().commit();
	      flash("success", "Customer has been deleted");
	      return redirect(routes.Customers.customers());
	  }
	
	public static void createNewEm(){
		entityManager = entityManagerFactory.createEntityManager();
		if(!entityManager.getTransaction().isActive()){
			entityManager.getTransaction().begin();
		}
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