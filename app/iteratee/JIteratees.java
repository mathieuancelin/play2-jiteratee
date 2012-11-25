package iteratee;

import org.codehaus.jackson.JsonNode;
import play.libs.Comet;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Results;
import play.mvc.WebSocket;

import java.io.UnsupportedEncodingException;

import static iteratee.F.*;
import static iteratee.Iteratees.*;

public class JIteratees {

    public static final String EVENTSOURCE = "text/event-stream";

    public static <T> Results.Status stream(final Enumerator<T> enumerator) {
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

    public static <T> Results.Status stream(final HubEnumerator<T> enumerator) {
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

    public static <T> Results.Status stream(final HubEnumerator<T> enumerator, final ByteBuilder<T> builder) {
        Results.Chunks<byte[]> chunks = new Results.ByteChunks() {
            public void onReady(final Results.Chunks.Out<byte[]> out) {
                out.onDisconnected(new play.libs.F.Callback0() {
                    @Override
                    public void invoke() throws Throwable {

                    }
                });
                enumerator.add(Iteratees.Iteratee.foreach(new Function<T, Unit>() {
                    @Override
                    public Unit apply(T s) {
                        out.write(builder.build(s));
                        return Unit.unit();
                    }
                }));
            }
        };
        Controller.response().setHeader("Content-Length", "-1");
        return Controller.ok(chunks);
    }

    public static <T> Results.Status stream(final Enumerator<T> enumerator, final ByteBuilder<T> builder) {
        Results.Chunks<byte[]> chunks = new Results.ByteChunks() {
            public void onReady(final Results.Chunks.Out<byte[]> out) {
                out.onDisconnected(new play.libs.F.Callback0() {
                    @Override
                    public void invoke() throws Throwable {

                    }
                });
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

    public static <T> Results.Status eventSource(final Enumerator<T> enumerator, final StrBuilder<T> builder) {
        Results.Chunks<String> chunks = new Results.StringChunks() {
            public void onReady(final Results.Chunks.Out<String> out) {
                out.onDisconnected(new play.libs.F.Callback0() {
                    @Override
                    public void invoke() throws Throwable {

                    }
                });
                enumerator.applyOn(Iteratee.foreach(new Function<T, Unit>() {
                    @Override
                    public Unit apply(T s) {
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

    public static <T> Results.Status eventSource(final HubEnumerator<T> enumerator, final StrBuilder<T> builder) {
        Results.Chunks<String> chunks = new Results.StringChunks() {
            public void onReady(final Results.Chunks.Out<String> out) {
                out.onDisconnected(new play.libs.F.Callback0() {
                    @Override
                    public void invoke() throws Throwable {

                    }
                });
                enumerator.add(Iteratee.foreach(new Function<T, Unit>() {
                    @Override
                    public Unit apply(T s) {
                        out.write("data: " + builder.build(s) + "\n\n");
                        return Unit.unit();
                    }
                }));
            }
        };
        Controller.response().setContentType(EVENTSOURCE);
        Controller.response().setHeader("Content-Length", "-1");
        return Controller.ok(chunks);
    }

    public static <T> Results.Status eventSource(final Enumerator<T> enumerator) {
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

    public static <T> Results.Status eventSource(final HubEnumerator<T> enumerator) {
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

    public static Enumeratee<Object, String> eventSource = Enumeratee.map(new Function<Object, String>() {
        @Override
        public String apply(Object s) {
            if (s instanceof JsonNode) {
                return "data: " + ((JsonNode) s).asText() + "\n\n";
            } else {
                return "data: " + s.toString() + "\n\n";
            }
        }
    });

    public static <T> Results.Status comet(String callback, final HubEnumerator<T> enumerator, final StrBuilder<T> builder) {
        Comet comet = new Comet(callback) {
            public void onConnected() {
                enumerator.add(Iteratees.Iteratee.foreach(new Function<T, Unit>() {
                    @Override
                    public Unit apply(T s) {
                        sendMessage(builder.build(s));
                        return Unit.unit();
                    }
                }));
            }
        };
        comet.onDisconnected(new play.libs.F.Callback0() {
            @Override
            public void invoke() throws Throwable {

            }
        });
        return Controller.ok(comet);
    }

    public static <T> Results.Status comet(String callback, final Enumerator<T> enumerator, final StrBuilder<T> builder) {
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
        /**comet.onDisconnected(new play.libs.F.Callback0() {
            @Override
            public void invoke() throws Throwable {

            }
        });**/
        return Controller.ok(comet);
    }

    public static <T> Results.Status comet(String callback, final Enumerator<T> enumerator) {
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

    public static <T> Results.Status comet(String callback, final HubEnumerator<T> enumerator) {
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

    public static <T> WebSocket<String> websocket(final Iteratee<String, Unit> inIteratee, final Enumerator<String> outEnumerator) {
        WebSocket<String> ws =  new WebSocket<String>() {
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
                in.onClose(new play.libs.F.Callback0() {
                    @Override
                    public void invoke() throws Throwable {

                    }
                });
            }
        };
        return ws;
    }

    public static <T> WebSocket<String> websocket(final Iteratee<String, Unit> inIteratee, final HubEnumerator<String> outEnumerator) {
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
                outEnumerator.add(send);
            }
        };
    }
}
