package no.ntnu.coap.gateway.proxy.http;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.protocol.*;

import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpClientPool {
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

    public static CloseableHttpAsyncClient createClient() {
        try {
            final CloseableHttpAsyncClient client = HttpAsyncClientBuilder.create()
                    .disableCookieManagement()
                    .setDefaultRequestConfig(createConnConfig())
                    .setConnectionManager(createPoolingConnManager())
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
            client.start();
            return client;
        } catch (IOReactorException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            return null;
        }
    }

    private static RequestConfig createConnConfig() {
        return RequestConfig.custom()
                .setConnectionRequestTimeout(5000)
                .setConnectTimeout(1000)
                .setSocketTimeout(500).build();
    }

    private static PoolingNHttpClientConnectionManager createPoolingConnManager() throws IOReactorException {
        IOReactorConfig config = IOReactorConfig.DEFAULT;
        ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(config);

        PoolingNHttpClientConnectionManager cm = new PoolingNHttpClientConnectionManager(ioReactor);
        cm.setMaxTotal(50);
        cm.setDefaultMaxPerRoute(50);

        return cm;
    }
}
