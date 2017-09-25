package no.ntnu.coap.gateway;

import no.ntnu.coap.gateway.proxy.resources.ProxyCoapClientResource;
import org.apache.commons.cli.*;
import org.eclipse.californium.core.CoapServer;
import no.ntnu.coap.gateway.proxy.DirectProxyCoapResolver;
import no.ntnu.coap.gateway.proxy.ProxyHttpServer;
import no.ntnu.coap.gateway.proxy.resources.ForwardingResource;
import no.ntnu.coap.gateway.proxy.resources.ProxyHttpClientResource;

import java.io.IOException;

/**
 * Main entry class for the HTTP server.
 */
public class App {
    public static void main(String[] args) throws IOException {
        CommandLineParser parser = new DefaultParser();
        String mode;
        Integer coapPort, httpPort;

        try {
            final CommandLine cli = parser.parse(getCliOptions(), args);

            mode = cli.getOptionValue("mode");
            coapPort = Integer.valueOf(cli.getOptionValue("coapPort", "5683"));
            httpPort = Integer.valueOf(cli.getOptionValue("httpPort", "8080"));
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            return;
        }

        final ProxyCoapClientResource coap2coap = new ProxyCoapClientResource("coap2coap");

        if (mode.equals("http")) {
            System.out.println("Starting HTTP gateway on port " + httpPort);

            final ProxyHttpServer httpServer = new ProxyHttpServer(httpPort);
            httpServer.setProxyCoapResolver(new DirectProxyCoapResolver(coap2coap));
        } else if (mode.equals("coap")) {
            System.out.println("Starting CoAP gateway on port " + coapPort);

            final ForwardingResource coap2http = new ProxyHttpClientResource("coap2http");

            // Create CoAP Server on PORT with proxy resources form CoAP to CoAP and HTTP
            final CoapServer targetServerA = new CoapServer(coapPort);
            targetServerA.add(coap2coap);
            targetServerA.add(coap2http);
            targetServerA.start();
        }
    }

    private static Options getCliOptions() {
        final Options options = new Options();

        options.addOption(Option.builder("mode")
                .hasArg()
                .longOpt("mode")
                .desc("Whether the proxy should be listening for CoAP or HTTP requests")
                .required()
                .build());

        options.addOption(Option.builder("coapPort")
                .hasArg()
                .longOpt("coapPort")
                .desc("UDP port to listen for CoAP requests")
                .type(Integer.class)
                .build());

        options.addOption(Option.builder("httpPort")
                .hasArg()
                .longOpt("httpPort")
                .desc("Port to listen for HTTP requests")
                .type(Integer.class)
                .build());

        options.addOption(Option.builder("proxyPass")
                .hasArg()
                .longOpt("proxyPass")
                .desc("Where to forward requests")
                .build());

        return options;
    }
}
