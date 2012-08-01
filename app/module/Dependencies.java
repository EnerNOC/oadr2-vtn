package module;

import com.google.inject.Provides;
import javax.inject.Singleton;
import service.*;

public class Dependencies {
    
  @Provides
  @Singleton
  public HttpService makeHttpSerivce(){
      return new HttpService();
  }


}