package iteratee;

import org.codehaus.jackson.JsonNode;
import play.libs.Comet;
import play.mvc.Controller;
import play.mvc.Results;
import play.mvc.WebSocket;

import java.io.UnsupportedEncodingException;

import static iteratee.F.*;
import static iteratee.Iteratees.*;

public class JIteratee {

    public static final String EVENTSOURCE = "text/event-stream";

    public static <T> play.mvc.Results.Status stream(final Iteratees.Enumerator<T> enumerator) {
        return stream(enumerator, new ByteBuilder<T>() {
            @Override
            public byte[] build(T value) {
                try {
                    return value.toString().getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return new byte[0];
                }
            }
        });
    }

    public static <T> play.mvc.Results.Status stream(final Iteratees.Enumerator<T> enumerator, final ByteBuilder<T> builder) {
        Results.Chunks<byte[]> chunks = new Results.ByteChunks() {
            public void onReady(final Results.Chunks.Out<byte[]> out) {
                enumerator.applyOn(Iteratees.Iteratee.foreach(new Function<T, Unit>() {
                    @Override
                    public Unit apply(T s) {
                        out.write(builder.build(s));
                        return Unit.unit();
                    }
                })).onRedeem(new Action<Promise<Unit>>() {
                    @Override
                    public void apply(Promise<Unit> unitPromise) {
                        out.close();
                    }
                });
            }
        };
        Controller.response().setHeader("Content-Length", "-1");
        return Controller.ok(chunks);
    }

    public static interface ByteBuilder<T>  {
        public byte[] build(T value);
    }
    public static interface StrBuilder<T>  {
        public String build(T value);
    }

    public static <T> play.mvc.Results.Status eventSource(final Iteratees.Enumerator<T> enumerator, final StrBuilder<T> builder) {
        Results.Chunks<String> chunks = new Results.StringChunks() {
            public void onReady(final Results.Chunks.Out<String> out) {
                enumerator.applyOn(Iteratee.foreach(new Function<T, Unit>() {
                    @Override
                    public Unit apply(T s) {
                        System.out.println("writing " + builder.build(s));
                        out.write("data: " + builder.build(s) + "\n\n");
                        return Unit.unit();
                    }
                })).onRedeem(new Action<Promise<Unit>>() {
                    @Override
                    public void apply(Promise<Unit> unitPromise) {
                        out.close();
                    }
                });
            }
        };
        Controller.response().setContentType(EVENTSOURCE);
        Controller.response().setHeader("Content-Length", "-1");
        return Controller.ok(chunks);
    }

    public static <T> play.mvc.Results.Status eventSource(final Iteratees.Enumerator<T> enumerator) {
        return eventSource(enumerator, new StrBuilder<T>() {
            @Override
            public String build(T value) {
                if (value instanceof JsonNode) {
                    return ((JsonNode) value).asText();
                } else {
                    return value.toString();
                }
            }
        });
    }

    public static Enumeratee<Object, String> eventSource = Iteratees.Enumeratee.map(new Function<Object, String>() {
        @Override
        public String apply(Object s) {
            if (s instanceof JsonNode) {
                return "data: " + ((JsonNode) s).asText() + "\n\n";
            } else {
                return "data: " + s.toString() + "\n\n";
            }
        }
    });

    public static <T> play.mvc.Results.Status comet(String callback, final Iteratees.Enumerator<T> enumerator, final StrBuilder<T> builder) {
        Comet comet = new Comet(callback) {
            public void onConnected() {
                enumerator.applyOn(Iteratees.Iteratee.foreach(new Function<T, Unit>() {
                    @Override
                    public Unit apply(T s) {
                        sendMessage(builder.build(s));
                        return Unit.unit();
                    }
                })).onRedeem(new Action<Promise<Unit>>() {
                    @Override
                    public void apply(Promise<Unit> unitPromise) {
                        close();
                    }
                });
            }
        };
        return Controller.ok(comet);
    }

    public static <T> play.mvc.Results.Status comet(String callback, final Iteratees.Enumerator<T> enumerator) {
        return comet(callback, enumerator, new StrBuilder<T>() {
            @Override
            public String build(T value) {
                if (value instanceof JsonNode) {
                    return ((JsonNode) value).asText();
                } else {
                    return value.toString();
                }
            }
        });
    }

    public static <T> WebSocket<String> websocket(final Iteratees.Iteratee<String, Unit> inIteratee, final Iteratees.Enumerator<String> outEnumerator) {
        return new WebSocket<String>() {
            public void onReady(final WebSocket.In<String> in, final WebSocket.Out<String> out) {
                final Iteratee<String, Unit> send = Iteratee.foreach(new Function<String, Unit>() {
                    @Override
                    public Unit apply(String s) {
                        out.write(s);
                        return Unit.unit();
                    }
                });
                final PushEnumerator<String> push = Enumerator.unicast(String.class);
                in.onMessage(new play.libs.F.Callback<String>() {
                    public void invoke(String event) {
                        push.push(event);
                    }
                });
                in.onClose(new play.libs.F.Callback0() {
                    public void invoke() {
                        push.stop();
                    }
                });
                push.applyOn(inIteratee);
                outEnumerator.applyOn(send);
            }
        };
    }
}
