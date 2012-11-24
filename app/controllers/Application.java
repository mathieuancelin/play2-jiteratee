package controllers;

import play.mvc.*;

import iteratee.F;
import iteratee.Iteratees;
import iteratee.JIteratee;

import java.util.concurrent.TimeUnit;

public class Application extends Controller {

    public static Result index() {
        return ok(views.html.index.render(""));
    }
  
    public static Result comet() {
        return JIteratee.comet("parent.cometMessage", Iteratees.Enumerator.generate(1, TimeUnit.SECONDS, new F.Function<F.Unit, F.Option<String>>() {
            @Override
            public F.Option<String> apply(F.Unit unit) {
                return F.Option.some(System.currentTimeMillis() + "");
            }
        }));
    }

    public static Result sse() {
        return JIteratee.eventSource(Iteratees.Enumerator.generate(1, TimeUnit.SECONDS, new F.Function<F.Unit, F.Option<String>>() {
            @Override
            public F.Option<String> apply(F.Unit unit) {
                return F.Option.some(System.currentTimeMillis() + "");
            }
        }));
    }

    public static Result stream() {
        return JIteratee.stream(Iteratees.Enumerator.generate(1, TimeUnit.SECONDS, new F.Function<F.Unit, F.Option<String>>() {
            @Override
            public F.Option<String> apply(F.Unit unit) {
                return F.Option.some(System.currentTimeMillis() + "\n");
            }
        }));
    }

    public static WebSocket<String> websocket() {
        final Iteratees.PushEnumerator<String> out = Iteratees.Enumerator.unicast(String.class);
        final Iteratees.Iteratee<String, F.Unit> in = Iteratees.Iteratee.foreach(new F.Function<String, F.Unit>() {
            @Override
            public F.Unit apply(String s) {
                out.push("Received : " + s);
                return F.Unit.unit();
            }
        });
        return JIteratee.websocket(in, out);
    }
}