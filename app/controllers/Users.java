package controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.enernoc.open.oadr2.model.EiEvent;

import models.ProjectEventRelation;
import models.ProjectForm;
import models.UserForm;
import play.Logger;
import play.data.Form;
import play.data.validation.ValidationError;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;

//export PATH=$PATH:/Users/jlajoie/Documents/play-2.0.1

/*
 * Delete is not working yet, sends some strange error about no data being sent and stuff
 */
public class Users extends Controller {
	static EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("Events");
	static EntityManager entityManager = entityManagerFactory.createEntityManager();
	

	public static Result index() {
		  return redirect(routes.Users.users());
	}

	@SuppressWarnings("unchecked")
	public static Result users(){
		  createNewEm();
		  List<UserForm> blankArray = entityManager.createQuery("FROM Users").getResultList();
		  for(UserForm user : blankArray){
			  user.programName = entityManager.find(ProjectForm.class, Long.parseLong(user.getProjectId())).getProjectName();
		  }
		  return ok(views.html.users.render(blankArray));
	}
	
	public static Result blankUser(){
		return ok(views.html.newUser.render(form(UserForm.class), makeProjectMap()));
	}
	
	@Transactional
	public static Result newUser(){
		  Form<UserForm> filledForm = form(UserForm.class).bindFromRequest();
		  if(filledForm.hasErrors()){
	    	  addFlashError(filledForm.errors());
			  return badRequest(views.html.newUser.render(filledForm, makeProjectMap()));
		  }
		  else{
			  createNewEm();
			  UserForm newUser = filledForm.get();entityManager.persist(newUser);
			  entityManager.getTransaction().commit();
			  flash("success", "Program as been created");
		  }
		  return redirect(routes.Users.users());
	}
	
	@SuppressWarnings("unchecked")
	@Transactional(readOnly=true)
	public static Map<String, String> makeProjectMap(){
		createNewEm();
		List<ProjectForm> projectList = entityManager.createQuery("FROM Project").getResultList();
		Map<String, String> projectMap = new HashMap<String, String>();
		for(ProjectForm project : projectList){
			projectMap.put(project.getId() + "", project.getProjectName());
		}
		
		return projectMap;
	}
	
	  @Transactional
	  //Deletes an event based on the id
	  public static Result deleteUser(Long id){
		  createNewEm();
		  entityManager.remove(entityManager.find(UserForm.class, id));
		  entityManager.getTransaction().commit();
	      flash("success", "Customer has been deleted");
	      return redirect(routes.Users.users());
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