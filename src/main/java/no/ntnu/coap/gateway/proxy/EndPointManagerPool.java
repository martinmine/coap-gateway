package no.ntnu.coap.gateway.proxy;

import org.eclipse.californium.core.network.EndpointManager;

import java.util.*;
import java.util.logging.Logger;

public class EndPointManagerPool {
    private static final int INIT_SIZE = 40;
    private static final Queue<EndpointManager> managers = initManagerPool(INIT_SIZE);

    private static final Logger LOGGER = Logger.getLogger(EndPointManagerPool.class.getName());

    private static Queue<EndpointManager> initManagerPool(final int size) {
        final Queue<EndpointManager> clients = new ArrayDeque<>(size);

        for (int i = 0; i < size; i++) {
            clients.add(createManager());
        }

        return clients;
    }

    public static EndpointManager getManager() {
        synchronized (managers) {
            if (managers.size() > 0) {
                return managers.remove();
            }
        }

        LOGGER.warning("Out of endpoint managers, creating more");
        return createManager();
    }

    private static EndpointManager createManager() {
        return new EndpointManager();
    }

    public static void putClient(final EndpointManager manager) {
        if (manager == null) return;

        synchronized (managers) {
            managers.add(manager);

            if (managers.size() > INIT_SIZE) {
                LOGGER.warning("Beyond pool capacity, current count: " + managers.size());
            }
        }
    }
}
