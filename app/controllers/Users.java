package controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import models.ProjectForm;
import models.UserForm;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;

//export PATH=$PATH:/Users/jlajoie/Documents/play-2.0.1

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
		  return ok(views.html.users.render(blankArray));
	}
	
	public static Result blankUser(){
		return ok(views.html.newUser.render(form(UserForm.class), makeProjectMap()));
	}
	
	@Transactional
	public static Result newUser(){
		  createNewEm();
		  Form<ProjectForm> filledForm = form(ProjectForm.class).bindFromRequest();
		  if(filledForm.hasErrors()){
			  //do some error handling
			  return badRequest();
		  }
		  else{
			  ProjectForm newProject = filledForm.get();
			  //Logger.info(newProject.getProjectName() + " " + newProject.getProjectURI() + " " + newProject.getId());
			  entityManager.persist(newProject);
			  entityManager.getTransaction().commit();
			  flash("success", "Program as been created");
		  }
		  return redirect(routes.Markets.projects());
	}
	
	@SuppressWarnings("unchecked")
	@Transactional(readOnly=true)
	public static Map<String, String> makeProjectMap(){
		  List<ProjectForm> projectList = entityManager.createQuery("FROM Project").getResultList();
		  Map<String, String> projectMap = new HashMap<String, String>();
		  for(ProjectForm project : projectList){
			  projectMap.put(project.getId() + "", project.getProjectName());
		  }
		  return projectMap;
	}
	
	public static Result deleteUser(long id){
		return TODO;
	}
	
	public static void createNewEm(){
		entityManager = entityManagerFactory.createEntityManager();
		if(!entityManager.getTransaction().isActive()){
			entityManager.getTransaction().begin();
		}
	}
}