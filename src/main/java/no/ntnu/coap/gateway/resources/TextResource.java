package no.ntnu.coap.gateway.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * Simple hello world text resource.
 */
@Path("text")
public class TextResource {

    @GET
    public String getText() {
        return "Hello world";
    }
}
