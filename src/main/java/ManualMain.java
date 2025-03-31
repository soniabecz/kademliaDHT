import java.net.InetAddress;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import protocols.apps.InteractiveApp;
import protocols.dht.KademliaDHT;
import protocols.point2point.Point2PointComm;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.network.data.Host;
import utils.InterfaceToIp;


public class ManualMain {

    //Sets the log4j (logging library) configuration file
    static {
        System.setProperty("log4j.configurationFile", "C:\\Users\\Sonia\\Desktop\\ASD\\asd2024-proj1-main (1)\\asd2024-proj1-main\\log4j2.xml");
    }

    //Creates the logger object
    private static final Logger logger = LogManager.getLogger(ManualMain.class);

    //Default babel configuration file (can be overridden by the "-config" launch argument)
    private static final String DEFAULT_CONF = "C:\\Users\\Sonia\\Desktop\\ASD\\asd2024-proj1-main (1)\\asd2024-proj1-main\\babel_config.properties";

    public static void main(String[] args) throws Exception {

        //Get the (singleton) babel instance
        Babel babel = Babel.getInstance();

        //Loads properties from the configuration file, and merges them with properties passed in the launch arguments
        Properties props = Babel.loadConfig(args, DEFAULT_CONF);

        //If you pass an interface name in the properties (either file or arguments), this wil get the IP of that interface
        //and create a property "address=ip" to be used later by the channels.
        InterfaceToIp.addInterfaceIp(props);

        //The Host object is an address/port pair that represents a network host. It is used extensively in babel
        //It implements equals and hashCode, and also includes a serializer that makes it easy to use in network messages
        Host dhtHost =  new Host(InetAddress.getByName(props.getProperty("address")),
                Integer.parseInt(props.getProperty("port")));

        Host commHost = new Host(InetAddress.getByName(props.getProperty("address")),
                Integer.parseInt(props.getProperty("port"))+ 1);

        logger.info("Hello, I am {}", dhtHost);

        // Application
        InteractiveApp interactiveApp = new InteractiveApp(props, Point2PointComm.PROTOCOL_ID);


        //DHT Protocol
        KademliaDHT dhtProto = new KademliaDHT();

        //Point-to-Point Communication Protocol
        Point2PointComm commProto = new Point2PointComm(commHost, KademliaDHT.PROTOCOL_ID, props, dhtProto);

        //Register applications in babel
        babel.registerProtocol(interactiveApp);

        //Register protocols
        babel.registerProtocol(dhtProto);
        babel.registerProtocol(commProto);

        //Init the protocols. This should be done after creating all protocols, since there can be inter-protocol
        //communications in this step.
        interactiveApp.init(props);
        dhtProto.init(props);
        commProto.init(props);


        //Start babel and protocol threads
        babel.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> logger.info("Goodbye")));

    }

}
