package org.fusesource.cloudlaunch.distribution.registry.zk;

import org.fusesource.cloudlaunch.distribution.registry.Registry;
import org.fusesource.cloudlaunch.distribution.registry.RegistryFactory;
import org.fusesource.cloudlaunch.util.internal.IntrospectionSupport;
import org.fusesource.cloudlaunch.util.internal.URISupport;

import java.net.URI;
import java.util.Map;

/**
 * @author chirino
 */
public class ZooKeeperFactory implements RegistryFactory {

    public Registry createRegistry(String uri) throws Exception {
        URI connectUri = new URI(URISupport.stripPrefix(uri, "zk:"));

        ZooKeeperRegistry registry = new ZooKeeperRegistry();

        //Use query params to initialize the factory:
        Map<String, String> props = URISupport.parseParamters(connectUri);
        if (!props.isEmpty()) {
            IntrospectionSupport.setProperties(registry, URISupport.parseQuery(uri));
            connectUri = URISupport.removeQuery(new URI(uri));
            connectUri = URISupport.createRemainingURI(connectUri, props);
        }
        registry.setConnectUrl(connectUri.toString());
        registry.start();
        return registry;

    }

}
