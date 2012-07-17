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
		
		/*
		//shouldn't have to hard code these, should check to see if can parse from XPath
		//also shouldnt name it happyMappy but it made me smile =D
		Map<String, String> happyMappy = new HashMap<String, String>();
		happyMappy.put("pyld", "http://docs.oasis-open.org/ns/energyinterop/201110/payloads");
		happyMappy.put("oadr", "http://openadr.org/oadr-2.0a/2012/03");
		happyMappy.put("emix", "http://docs.oasis-open.org/ns/emix/2011/06");
		happyMappy.put("ei", "http://docs.oasis-open.org/ns/energyinterop/201110");
		happyMappy.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		//Logger.info(XPath.selectText("//emix:marketContext", document, happyMappy));
		*/
		
		JAXBContext jaxbContext = JAXBContext.newInstance("org.enernoc.open.oadr2.model");
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		Marshaller marshaller = jaxbContext.createMarshaller();
		Object o = unmarshaller.unmarshal(document);
		
		//should make all instance ofs own methods probably, also still not do instanceof cause bad
		
		//if RequestEvent send out a response!
		if(o instanceof OadrRequestEvent){
			OadrRequestEvent oRequestEvent = (OadrRequestEvent) o;
			EiResponse e = new EiResponse();
			e.setRequestID(oRequestEvent.getEiRequestEvent().getRequestID());
			/*
			 * 1xx Informational - Request received, continuing process
			 * 2xx Success - The request was successfully received, understood, and accepted
			 * 3xx Pending - Further action must be taken in order to complete the request
			 * 4xx Requester Error - The request contains bad syntax or cannot be fulfilled
			 * 5xx Responder Error - The responder failed to fulfill an apparently valid request
			 */
			//be sure to do error handling if it is not a 1 or 2 first char type thing
			e.setResponseCode("000");//shouldnt be 000, check out 5.6 of the EiResponse page
			e.setResponseDescription("Optional! But I'm here <3");
			Object response = new OadrResponse().withEiResponse(e);
			
			StringWriter sw = new StringWriter();
			marshaller.marshal(response, sw);
			response().setContentType("application/xml");
			return ok(sw.toString() + '\n');
		}
		//if its a created event send out a distribute event
		else if(o instanceof OadrCreatedEvent){
			OadrCreatedEvent oCreatedEvent = (OadrCreatedEvent) o;
			//check the oCreatedEvent.eiresponse for an OKAY message
			//then check the other eventresponses for okay responses
			OadrDistributeEvent distributeEvent = new OadrDistributeEvent();
			EiResponse eiResponse = new EiResponse();
			
			eiResponse.withRequestID(oCreatedEvent.getEiCreatedEvent().getEiResponse().getRequestID());		
			
			//eiResponse.withResponseCode(null);
			//eiResponse.withResponseDescription(null);			
			//response.withEiResponse(null);
			//response.withOadrEvent(null);
			
			//not sure if should set the vtn to the id the created event comes from
			//response.withVtnID(oCreatedEvent.getEiCreatedEvent().getVenID());
			distributeEvent.withEiResponse(eiResponse);
			distributeEvent.withRequestID(oCreatedEvent.getEiCreatedEvent().getEiResponse().getRequestID());
			
			StringWriter sw = new StringWriter();
			marshaller.marshal(distributeEvent, sw);
			response().setContentType("application/xml");
			return ok(sw.toString() + '\n');
		}
		//if its a distribute event...
		/*
		else if(o instanceof OadrDistributeEvent){
			OadrDistributeEvent oDistributeEvent = (OadrDistributeEvent) o;
		}
		*/
		
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
