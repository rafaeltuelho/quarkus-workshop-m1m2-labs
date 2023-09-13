package org.acme.people.stream;

import java.util.concurrent.ThreadLocalRandom;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.reactive.messaging.annotations.Broadcast;

@ApplicationScoped
public class NameConverter {
    
    public static final String[] honorifics = {"Mr.", "Mrs.", "Sir", "Madam", "Lord", "Lady", "Dr.", "Professor", "Vice-Chancellor", "Regent", "Provost", "Prefect"};


    @Incoming("names")
    @Outgoing("my-data-stream")
    @Broadcast
    public String process(String name) {
        String honorific = honorifics[ThreadLocalRandom.current().nextInt(honorifics.length)];
        return honorific + " " + name;
    }
}
