package no.ntnu.coap.gateway;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;

import javax.ws.rs.ServiceUnavailableException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

/**
 * Helper class for instantiating HTTP requests.
 */
public class HTTPHelper {
    private static final int INIT_SIZE = 10;
    private static final Queue<CloseableHttpAsyncClient> clients = initClientPool(INIT_SIZE);

    private static Queue<CloseableHttpAsyncClient> initClientPool(final int initSize) {
        long initTimeStart = System.currentTimeMillis();
        Queue<CloseableHttpAsyncClient> clients = new ArrayDeque<>(initSize);

        for (int i = 0; i < initSize; i++) {
            clients.add(HttpAsyncClients.createDefault());
        }

        long initTimeSpent = System.currentTimeMillis() - initTimeStart;
        System.out.println("Spent " + initTimeSpent + "ms on initing pool");

        return clients;
    }

    /**
     * Gets a client from the pool.
     * @return Client.
     */
    private static CloseableHttpAsyncClient getClient() {
        synchronized (clients) {
            if (clients.size() == 0) {
                System.out.println("WARNING: Pool empty! Returning new client");
                return HttpAsyncClients.createDefault();
            } else {
                return clients.remove();
            }
        }
    }

    /**
     * Adds a client back to the pool.
     * @param client Client to put back to the pool.
     */
    private static void freeClient(final CloseableHttpAsyncClient client) throws IOException {
        synchronized (clients) {
            if (clients.size() >= INIT_SIZE) {
                client.close();
            } else {
                clients.add(client);
            }
        }
    }

    public static CompletableFuture<String> get(final String path) {
        final CompletableFuture<String> future = new CompletableFuture<>();
        final CloseableHttpAsyncClient httpclient = getClient();
        httpclient.start();

        final HttpGet request = new HttpGet(path);
        httpclient.execute(request, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(final HttpResponse response) {
                try {
                    future.complete(EntityUtils.toString(response.getEntity()));
                    freeClient(httpclient);
                } catch (IOException e) {
                    future.completeExceptionally(e);
                }
            }

            @Override
            public void failed(final Exception ex) {
                try {
                    httpclient.close();
                } catch (IOException e) {
                    future.completeExceptionally(e);
                }
            }

            @Override
            public void cancelled() {
                future.completeExceptionally(new ServiceUnavailableException("Call cancelled"));
            }
        });

        return future;
    }
}
