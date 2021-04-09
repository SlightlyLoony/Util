import java.util.List;
import java.util.ArrayList;
import com.dilatush.test.Config;
import com.dilatush.test.Configurator;

public class Test implements Configurator {

    public void config( final Object _config ) {

        Config config = (Config) _config;

        config.x = 999;
        config.y.z = "this is only a test";

        Bogus bogus = new Bogus();
        System.out.println( "bogus" );
    }
}