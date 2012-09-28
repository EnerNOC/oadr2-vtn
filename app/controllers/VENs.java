package controllers;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Persistence;

import models.Program;
import models.VEN;
import play.Logger;
import play.data.Form;
import play.data.validation.ValidationError;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;

//export PATH=$PATH:/Users/jlajoie/Documents/play-2.0.1

public class VENs extends Controller {	

	public static Result index() {
		  return redirect(routes.VENs.vens());
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public static Result vens(){
		  List<VEN> vens = JPA.em().createQuery("FROM VEN").getResultList();
		  		  
		  class VENFormComparator implements Comparator<VEN>{
				public int compare(VEN userOne, VEN userTwo){
					return userOne.getVenID().compareTo(userTwo.getVenID());
				}
			}
		  
		  Collections.sort(vens, new VENFormComparator());
		  return ok(views.html.vens.render(vens));
	}
	
	public static Result blankVEN(){
		return ok(views.html.newVEN.render(form(VEN.class), makeProgramMap()));
	}
	
	@Transactional
	public static Result newVEN(){
		  Form<VEN> filledForm = form(VEN.class).bindFromRequest();
		  if(filledForm.hasErrors()){
	    	  addFlashError(filledForm.errors());
			  return badRequest(views.html.newVEN.render(filledForm, makeProgramMap()));
		  }
		  else{
			  VEN newVEN = filledForm.get();
			  Logger.info("Program id is: " + newVEN.getProgramId());
			  newVEN.setProgramId(JPA.em().find(Program.class, Long.parseLong(newVEN.getProgramId())).getProgramName());
			  JPA.em().persist(newVEN);
			  flash("success", "VEN as been created");
		  }
		  return redirect(routes.VENs.vens());
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
	  public static Result deleteVEN(Long id){
		  JPA.em().remove(JPA.em().find(VEN.class, id));
	      flash("success", "VEN has been deleted");
	      return redirect(routes.VENs.vens());
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