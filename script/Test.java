import com.dilatush.test.Config;
import com.dilatush.util.config.Configurator;
import com.dilatush.util.config.AConfig;

public class Test implements Configurator {

    public void config( final AConfig _config ) {

        Config config = (Config) _config;

        config.x = 999;
        config.y = <<secret>>;
        config.z.z = "what";
    }
}