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

        Enumeratee<Event, Event> secure = Enumeratee.map(new Function<Event, Event>() {
            @Override
            public Event apply(Event o) {
                for (SystemStatus status : caseClassOf(SystemStatus.class, o)) {
                    if (role.equals("MANAGER")) {
                        return status;
                    }
                }
                for (Operation operation : caseClassOf(Operation.class, o)) {
                    if (operation.level.equals("public")) {
                        return operation;
                    } else {
                        if (role.equals("MANAGER")) {
                            return operation;
                        }
                    }
                }
                return null;
            }
        });

        Enumeratee<Event, Event> inBounds = Enumeratee.map(new F.Function<Event, Event>() {
            @Override
            public Event apply(Event o) {
                for (SystemStatus status : caseClassOf(SystemStatus.class, o)) {
                    return status;
                }
                for (Operation operation : caseClassOf(Operation.class ,o)) {
                    if (operation.amount > lowerBound && operation.amount < higherBound) {
                        return operation;
                    }
                }
                return null;
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
                return null;
            }
        });

        return JIteratees.eventSource(Streams.events.through(secure, inBounds).through(asJson));
    }
}