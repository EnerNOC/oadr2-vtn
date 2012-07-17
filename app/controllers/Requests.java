package controllers;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import models.ProjectForm;

import org.enernoc.open.oadr2.model.EiResponse;
import org.enernoc.open.oadr2.model.OadrCreatedEvent;
import org.enernoc.open.oadr2.model.OadrDistributeEvent;
import org.enernoc.open.oadr2.model.OadrRequestEvent;
import org.enernoc.open.oadr2.model.OadrResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


import play.db.jpa.Transactional;
import play.libs.XPath;
import play.Logger;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

public class Requests extends Controller{
		
	public static Result requests() {
		return ok(views.html.requests.render());
	}
	
	/* should likely break apart this method, too massive and does too much
	*  @BodyParser.Of(BodyParser.TolerantXml.class) parser doesn't work due to lack of
	*  namespace consistancy (eg expects <{http://openadr.org/oadr-2.0a/2012/03}oadrRequestEvent>
	*  but receives uri:"", local:"oadr:oadrRequestEvent")
	*/
	public static Result marshalRequest() throws JAXBException, IOException{
		Document document = null;
		try {
			document = loadXmlFromString(new ByteArrayInputStream(request().body().asRaw().asBytes()));
		} catch (Exception e) {
			return badRequest("XML is invalid.\n");
		}
		
		JAXBContext jaxbContext = JAXBContext.newInstance("org.enernoc.open.oadr2.model");
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		Marshaller marshaller = jaxbContext.createMarshaller();
		Object o = unmarshaller.unmarshal(document);
		
		//should make all instance ofs own methods probably, also still not do instanceof cause bad		
		//if RequestEvent send out a response!
		if(o instanceof OadrRequestEvent){						
			OadrRequestEvent oRequestEvent = (OadrRequestEvent) o;
			Logger.info(oRequestEvent.toString());
			EiResponse eiResponse = new EiResponse();
			/*
			 * 1xx Informational - Request received, continuing process
			 * 2xx Success - The request was successfully received, understood, and accepted
			 * 3xx Pending - Further action must be taken in order to complete the request
			 * 4xx Requester Error - The request contains bad syntax or cannot be fulfilled
			 * 5xx Responder Error - The responder failed to fulfill an apparently valid request
			 */

			eiResponse.setRequestID(oRequestEvent.getEiRequestEvent().getRequestID());
			eiResponse.setResponseCode("200");
			eiResponse.setResponseDescription("Optional! But I'm here <3");
						
			OadrDistributeEvent response = new OadrDistributeEvent().withEiResponse(eiResponse);
			//Need to find out how to get EiEvent(s) and add them to this Distribute Event
			//Possibly from the Market Context, but need XPath to access, or find through VEN and Users
			//response.withOadrEvent(null);
			response.withRequestID(eiResponse.getRequestID());
			
			StringWriter sw = new StringWriter();
			marshaller.marshal(response, sw);
			response().setContentType("application/xml");
			//probably should not return a string and find a way to format xml
			return ok(sw.toString() + '\n');
		}
		//if its a created event send out a distribute event
		else if(o instanceof OadrCreatedEvent){
			OadrCreatedEvent oCreatedEvent = (OadrCreatedEvent) o;
			
			//Be sure to set the response code correctly to 1-5 depending on how the event is handled, temp for now
			EiResponse eiResponse = new EiResponse()
				.withRequestID(oCreatedEvent.getEiCreatedEvent().getEiResponse().getRequestID())
				.withResponseCode("200")
				.withResponseDescription("Optional description!");						
			
			StringWriter sw = new StringWriter();
			marshaller.marshal(new OadrResponse(eiResponse), sw);
			response().setContentType("application/xml");
			return ok(sw.toString() + '\n');
		}
		//occurs after sending out a DistributeEvent
		else if(o instanceof OadrResponse){
			OadrResponse oResponse = (OadrResponse) o;
			//this is where the Comet/Websocket stuff will happen to populate the table!
		}
		
		return ok("Hallo! Things didn't error out, but should not have reached here!\n");
	} 
	
	//seems insanely stupid to have to parse an application/xml as bytes then go through the doc builder
	//in order to get a Document, would MUCH prefer to use .getXml() but is stupid and returns null 
	//as only works for text/xml
	public static Document loadXmlFromString(ByteArrayInputStream xmlByteStream) throws Exception{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder;
		builder = factory.newDocumentBuilder();			
		return builder.parse(xmlByteStream);
	}
	
	@Transactional
	public static ProjectForm getProgramFromUri(String uri){
		return null;
	}

	
}
