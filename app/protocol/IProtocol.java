package protocol;

import javax.persistence.Embeddable;

import org.enernoc.open.oadr2.model.EiEvent;
import org.enernoc.open.oadr2.model.OadrCreatedEvent;
import org.enernoc.open.oadr2.model.OadrDistributeEvent;
import org.enernoc.open.oadr2.model.OadrRequestEvent;
import org.enernoc.open.oadr2.model.OadrResponse;

import models.VEN;

@Embeddable
public interface IProtocol {
    
    public enum ProtocolType{ XMPP, HTTP }
    
    public void send(VEN vtn, OadrResponse oadrResponse);
    public void send(VEN vtn, EiEvent eiEvent);
    public void send(VEN vtn, OadrDistributeEvent oadrDistributeEvent);
    public void send(VEN vtn, OadrCreatedEvent oadrCreatedEvent);
    public void send(VEN vtn, OadrRequestEvent oadrRequestEvent);

}