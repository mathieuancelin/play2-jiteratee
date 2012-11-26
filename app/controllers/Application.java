package controllers;

import iteratee.JIteratees;
import play.mvc.*;

import iteratee.F;
import static iteratee.F.*;
import iteratee.Iteratees;
import static iteratee.Iteratees.*;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Application extends Controller {

    public static final AtomicInteger integer = new AtomicInteger(0);
    public static final PushEnumerator<String> enumerator = Enumerator.unicast(String.class);
    public static final HubEnumerator<String> hub = Enumerator.broadcast(enumerator);

    public static Result index() {
        return ok(views.html.index.render(""));
    }

    public static Result push() {
        enumerator.push(integer.incrementAndGet() + "");
        return ok();
    }
  
    public static Result comet() {
        return JIteratees.comet("parent.cometMessage", Iteratees.Enumerator.generate(1, TimeUnit.SECONDS, new F.Function<F.Unit, F.Option<String>>() {
            @Override
            public F.Option<String> apply(F.Unit unit) {
                return F.Option.some(System.currentTimeMillis() + "");
            }
        }));
    }

    public static Result ssePushed() {
        return JIteratees.eventSource(hub);
    }

    public static Result sse() {
        return JIteratees.eventSource(Iteratees.Enumerator.generate(1, TimeUnit.SECONDS, new F.Function<F.Unit, F.Option<String>>() {
            @Override
            public F.Option<String> apply(F.Unit unit) {
                return F.Option.some(System.currentTimeMillis() + "");
            }
        }));
    }

    public static Result stream() {
        return JIteratees.stream(Iteratees.Enumerator.generate(1, TimeUnit.SECONDS, new F.Function<F.Unit, F.Option<String>>() {
            @Override
            public F.Option<String> apply(F.Unit unit) {
                return F.Option.some(System.currentTimeMillis() + "\n");
            }
        }));
    }

    public static WebSocket<String> websocket() {
        final Iteratees.PushEnumerator<String> out = Iteratees.Enumerator.unicast(String.class);
        final Iteratees.Iteratee<String, F.Unit> in = Iteratees.Iteratee.foreach(new UFunction<String>() {
            public void invoke(String s) {
                out.push("Received : " + s);
            }
        });
        return JIteratees.websocket(in, out);
    }

    public static Result file() {
        return JIteratees.file(Enumerator.fromFile(new File("/tmp/stuff")));
    }
}