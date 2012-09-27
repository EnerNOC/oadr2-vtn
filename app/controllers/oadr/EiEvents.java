package controllers.oadr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import play.Logger;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import service.oadr.EiEventService;

import com.google.inject.Inject;

/**
 * Controller to respond for OADR requests with an XML payload
 * @author jlajoie
 *
 */
public class EiEvents extends Controller{
    

    static JAXBContext jaxbContext;
    static{
        try {
            JAXBContext.newInstance("org.enernoc.open.oadr2.model");
        } catch (JAXBException e) {
            Logger.warn("Exception thrown creating JAXBContext instance.", e);
        }
    }
    
    @Inject static EiEventService eiEventService;

    @Transactional
    //convert to XML from eiEventService
    public static Result sendHttpResponse() throws JAXBException{
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Object payload = unmarshaller.unmarshal(new ByteArrayInputStream(request().body().asRaw().asBytes()));
        Object eiResponse = eiEventService.handleOadrPayload(payload);
        Marshaller marshaller = jaxbContext.createMarshaller();
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        marshaller.marshal(eiResponse, outputStream);
        response().setContentType("application/xml");
        return(ok(outputStream.toByteArray()));
    }
    
    
}
