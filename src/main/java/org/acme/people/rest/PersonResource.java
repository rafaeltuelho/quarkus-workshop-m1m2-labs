package org.acme.people.rest;

import java.time.LocalDate;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.acme.people.model.DataTable;
import org.acme.people.model.EyeColor;
import org.acme.people.model.Person;
import org.acme.people.utils.CuteNameGenerator;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;

@Path("/person")
@ApplicationScoped
public class PersonResource {
    @Inject
    EventBus bus;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Person> getAll() {
        return Person.listAll();
    }

    @GET
    @Path("/eyes/{color}")
    public List<Person> findByColor(@PathParam("color") EyeColor color) {
        return Person.findByColor(color);
    }

    @GET
    @Path("/birth/before/{year}")
    @Operation(summary = "Finds people born before a specific year",
               description = "Search the people database and return a list of people born before the specified year")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "The list of people born before the specified year",
                        content = @Content(schema = @Schema(implementation = Person.class))),
        @APIResponse(responseCode = "500", description = "Something bad happened")
    })
    public List<Person> getBeforeYear(@PathParam("year") int year) {
        return Person.getBeforeYear(year);
    }

    @GET
    @Path("/datatable")
    @Produces(MediaType.APPLICATION_JSON)
    public DataTable datatable(
        @QueryParam(value = "draw") int draw,
        @QueryParam(value = "start") int start,
        @QueryParam(value = "length") int length,
        @QueryParam(value = "search[value]") String searchVal

        ) {
            // TODO: Begin result
            DataTable result = new DataTable();
            result.setDraw(draw);
            // TODO: Filter based on search
            PanacheQuery<Person> filteredPeople;

            if (searchVal != null && !searchVal.isEmpty()) { 
                filteredPeople = Person.<Person>find("name like :search",
                    Parameters.with("search", "%" + searchVal + "%"));
            } else {
                filteredPeople = Person.findAll();
            }
            // TODO: Page and return
            int page_number = start / length;
            filteredPeople.page(page_number, length);

            result.setRecordsFiltered(filteredPeople.count());
            result.setData(filteredPeople.list());
            result.setRecordsTotal(Person.count());

            return result;
    }

    @POST
    @Path("/{name}")
    public Uni<Person> addPerson(@PathParam("name") String name) {
        return bus.<Person>request("add-person", name)
            .onItem().transform(response -> response.body());
    }

    @GET
    @Path("/{name}")
    public Person byName(@PathParam("name") String name) {
        return Person.find("name", name).firstResult();
    }

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        for (int i = 0; i < 1000; i++) {
            String name = CuteNameGenerator.generate();
            LocalDate birth = LocalDate.now().plusWeeks(Math.round(Math.floor(Math.random() * 40 * 52 * -1)));
            EyeColor color = EyeColor.values()[(int)(Math.floor(Math.random() * EyeColor.values().length))];
            Person p = new Person();
            p.birth = birth;
            p.eyes = color;
            p.name = name;
            Person.persist(p);
        }
    }
}
