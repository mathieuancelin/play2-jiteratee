package controllers;

import iteratee.JIteratees;
import models.Streams;
import static models.Streams.*;
import play.mvc.Controller;
import play.mvc.Result;
import iteratee.F;
import static iteratee.F.*;
import iteratee.Iteratees;
import static iteratee.Iteratees.*;

public class RealTime extends Controller {

    public static Result index(String role) {
        return ok(views.html.realtime.render(role));
    }

    public static Result feed(final String role, final int lowerBound, final int higherBound) {

        Enumeratee<Event, Event> secure = Enumeratee.collect(new Function<Event, Option<Event>>() {
            @Override
            public Option<Event> apply(Event o) {
                for (SystemStatus status : caseClassOf(SystemStatus.class, o)) {
                    if (role.equals("MANAGER")) {
                        return Option.<Event>some(status);
                    }
                }
                for (Operation operation : caseClassOf(Operation.class, o)) {
                    if (operation.level.equals("public")) {
                        return Option.<Event>some(operation);
                    } else {
                        if (role.equals("MANAGER")) {
                            return Option.<Event>some(operation);
                        }
                    }
                }
                return Option.none();
            }
        });

        Enumeratee<Event, Event> inBounds = Enumeratee.collect(new Function<Event, Option<Event>>() {
            @Override
            public Option<Event> apply(Event o) {
                for (SystemStatus status : caseClassOf(SystemStatus.class, o)) {
                    return Option.<Event>some(status);
                }
                for (Operation operation : caseClassOf(Operation.class ,o)) {
                    if (operation.amount > lowerBound && operation.amount < higherBound) {
                        return Option.<Event>some(operation);
                    }
                }
                return Option.none();
            }
        });

        Enumeratee<Event, String> asJson = Enumeratee.map(new Function<Event, String>() {
            @Override
            public String apply(Event o) {
                for (SystemStatus status : caseClassOf(SystemStatus.class, o)) {
                    return "{\"type\":\"status\", \"message\":\"" + status.message + "\"}";
                }
                for (Operation operation : caseClassOf(Operation.class ,o)) {
                    return "{\"type\":\"operation\", \"amount\":" + operation.amount + ", \"visibility\":\"" + operation.level + "\"}";
                }
                return "";
            }
        });

        return JIteratees.eventSource(Streams.events.through(secure, inBounds).through(asJson));
    }
}