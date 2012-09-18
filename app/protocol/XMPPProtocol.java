package protocol;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.xml.bind.JAXBException;

import models.VEN;

import org.enernoc.open.oadr2.model.EiEvent;
import org.enernoc.open.oadr2.model.OadrCreatedEvent;
import org.enernoc.open.oadr2.model.OadrDistributeEvent;
import org.enernoc.open.oadr2.model.OadrDistributeEvent.OadrEvent;
import org.enernoc.open.oadr2.model.OadrRequestEvent;
import org.enernoc.open.oadr2.model.OadrResponse;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.PacketExtension;

import play.Logger;
import service.XmppService;
import test.JAXBManager;
import test.OADR2IQ;
import test.OADR2PacketExtension;

public class XMPPProtocol extends BaseProtocol{    
    
    XMPPService service = XMPPService.getInstance();    
    static EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("Events");
    static EntityManager entityManager = entityManagerFactory.createEntityManager();
    XMPPConnection connection;
    
    
    public XMPPProtocol(){
        this.setProtocolType(ProtocolType.XMPP);
    }

    @Override
    public void send(VEN vtn, OadrResponse oadrResponse) {
        
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void send(VEN vtn, EiEvent eiEvent){
        createNewEm();
        
        List<VEN> customers = entityManager.createQuery("SELECT c from Customers c WHERE c.programId = :uri")
                .setParameter("uri", eiEvent.getEventDescriptor().getEiMarketContext().getMarketContext())
                .getResultList();
        
        for(VEN customer: customers){
            OadrDistributeEvent distribute = new OadrDistributeEvent()
            .withOadrEvents(new OadrEvent().withEiEvent(eiEvent))
            .withVtnID(connection.getUser());
            IQ iq = null;
            try {
                iq = new OADR2IQ(new OADR2PacketExtension(distribute, new JAXBManager().createMarshaller()));
            } catch (JAXBException e) {
                Logger.warn("Exception thrown from XMPP send(eiEvent)");
            }
            iq.setTo(customer.getClientURI());
            //iq.setTo("xmpp-ven@msawant-mbp.local/msawant-mbp");
            iq.setType(IQ.Type.SET);
            //TODO Check out WHY this gives a feature-not-implemented error, makes NO sense
            connection.sendPacket(iq);      
        }        
    }
    
    @Override
    public void send(VEN vtn, OadrDistributeEvent oadrDistributeEvent) {
        try {
            service.connect();
        } catch (XMPPException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void send(VEN vtn, OadrCreatedEvent oadrCreatedEvent) {
    }

    @Override
    public void send(VEN vtn, OadrRequestEvent oadrRequestEvent) {
        
    }
    
    public static void createNewEm(){
        entityManager = entityManagerFactory.createEntityManager();
        if(!entityManager.getTransaction().isActive()){
            entityManager.getTransaction().begin();
        }
    }

}