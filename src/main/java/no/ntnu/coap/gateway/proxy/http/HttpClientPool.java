package no.ntnu.coap.gateway.proxy.http;

import org.apache.http.HttpResponse;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.protocol.*;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.logging.Logger;

public class HttpClientPool {
    private static final int INIT_SIZE = 10;
    private static final Queue<CloseableHttpAsyncClient> clients = initClientPool(INIT_SIZE);

    private static final int KEEP_ALIVE = 5000;

    private static final Logger LOGGER = Logger.getLogger(HttpClientPool.class.getName());

    /**
     * DefaultHttpClient is thread safe. It is recommended that the same
     * instance of this class is reused for multiple request executions.
     * NO IT IS NOT RECOMMENDED TO DO SO:
     * https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d5e619
     */


    private HttpClientPool() {
    }

    private static Queue<CloseableHttpAsyncClient> initClientPool(final int size) {
        final Queue<CloseableHttpAsyncClient> clients = new ArrayDeque<>(size);

        for (int i = 0; i < size; i++) {
            clients.add(createClient());
        }

        return clients;
    }

    public static CloseableHttpAsyncClient getClient() {
        synchronized (clients) {
            if (clients.size() > 0) {
                return clients.remove();
            }
        }

        LOGGER.warning("Out of clients, creating more");

        return createClient();
    }

    public static CloseableHttpAsyncClient createClient() {
        return HttpAsyncClients.custom()
                .addInterceptorFirst(new RequestAcceptEncoding())
                .addInterceptorFirst(new RequestConnControl())
                // .addInterceptorFirst(new RequestContent())
                .addInterceptorFirst(new RequestDate())
                .addInterceptorFirst(new RequestExpectContinue())
                .addInterceptorFirst(new RequestTargetHost())
                .addInterceptorFirst(new RequestUserAgent())
                .addInterceptorFirst(new ResponseContentEncoding())
                .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy() {
                    @Override
                    public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                        long keepAlive = super.getKeepAliveDuration(response, context);
                        if (keepAlive == -1) {
                            // Keep connections alive if a keep-alive value
                            // has not be explicitly set by the server
                            keepAlive = KEEP_ALIVE;
                        }
                        return keepAlive;
                    }

                })
                .build();
    }

    public static void putClient(final CloseableHttpAsyncClient client) {
        synchronized (clients) {
            if (clients.size() > INIT_SIZE) {
                try {
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                clients.add(client);
            }
        }
    }
}
