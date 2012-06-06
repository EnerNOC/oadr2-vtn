package controllers;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

//import org.enernoc.open.oadr2.model.OadrDistributeEvent.OadrEvent;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import play.Logger;
import play.mvc.*;

public class Application extends Controller {
	
	static Marshaller marshaller;
	Schema schema;
	static Unmarshaller unmarshaller;
	
		//@BodyParser.Of(Xml.class)
		public static Result sayHello() throws Exception{
			ByteArrayInputStream input = new ByteArrayInputStream(request().body().asRaw().asBytes());
			JAXBContext jaxbContext = JAXBContext.newInstance("org.enernoc.open.oadr2.model");
			unmarshaller = jaxbContext.createUnmarshaller();		
			marshaller = jaxbContext.createMarshaller();
			/*
			SAXParserFactory sax = SAXParserFactory.newInstance();
			sax.setNamespaceAware(true);					
			InputSource iSource = new InputSource(input);
			XMLReader reader = sax.newSAXParser().getXMLReader();
			SAXSource sSource = new SAXSource(reader, iSource);			
			Object o = unmarshaller.unmarshal(sSource);
			*/
			Object o = unmarshaller.unmarshal(input);
			Logger.info(o.toString() + '\n');
			StringWriter out = new StringWriter();
			
			marshaller.marshal(o, out);
			Logger.info(out.toString());
			return ok("Hello" + "\n");
		}
	
	}