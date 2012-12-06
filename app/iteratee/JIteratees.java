package iteratee;

import akka.actor.ActorRef;
import org.codehaus.jackson.JsonNode;
import play.libs.Comet;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Results;
import play.mvc.WebSocket;

import java.io.UnsupportedEncodingException;

import static iteratee.F.*;
import static iteratee.Iteratees.*;

public class JIteratees {

    public static final String EVENTSOURCE = "text/event-stream";

    public static <T> Results.Status file(final Enumerator<byte[]> enumerator) {
        return file(enumerator, "application/octet-stream");
    }

    public static <T> Results.Status file(final Enumerator<byte[]> enumerator, String contentType) {
        Results.Chunks<byte[]> chunks = new Results.ByteChunks() {
            public void onReady(final Results.Chunks.Out<byte[]> out) {
                out.onDisconnected(new play.libs.F.Callback0() {
                    @Override
                    public void invoke() throws Throwable {}
                });
                enumerator.applyOn(Iteratees.Iteratee.foreach(new Function<byte[], Unit>() {
                    @Override
                    public Unit apply(byte[] s) {
                        out.write(s);
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
                    public void invoke() throws Throwable {}
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
                    public void invoke() throws Throwable {}
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

    public static interface ByteBuilder<T> extends Builder<T, byte[]> {
        public byte[] build(T value);
    }
    public static interface StrBuilder<T> extends Builder<T, String> {
        public String build(T value);
    }

    public static interface BuilderFromStr<T> extends Builder<String, T>{
        public T build(String value);
    }

    public static interface Builder<I, O> {
        public O build(I value);
    }

    public static class Identity<T> implements Builder<T, T> {
        @Override
        public T build(T value) {
            return value;
        }
    }

    public static <T> Results.Status eventSource(final Enumerator<T> enumerator, final StrBuilder<T> builder) {
        Results.Chunks<String> chunks = new Results.StringChunks() {
            public void onReady(final Results.Chunks.Out<String> out) {
                out.onDisconnected(new play.libs.F.Callback0() {
                    @Override
                    public void invoke() throws Throwable {}
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
                    public void invoke() throws Throwable {}
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
                    return Json.stringify((JsonNode) value);
                } else {
                    return /**org.apache.commons.lang3.StringEscapeUtils.escapeEcmaScript(**/value.toString();
                }
            }
        });
    }

    public static <T> Results.Status eventSource(final HubEnumerator<T> enumerator) {
        return eventSource(enumerator, new StrBuilder<T>() {
            @Override
            public String build(T value) {
                if (value instanceof JsonNode) {
                    return Json.stringify((JsonNode) value);
                } else {
                    return /**org.apache.commons.lang3.StringEscapeUtils.escapeEcmaScript(**/value.toString();
                }
            }
        });
    }

    public static Enumeratee<Object, String> eventSource = Enumeratee.map(new Function<Object, String>() {
        @Override
        public String apply(Object s) {
            if (s instanceof JsonNode) {
                return "data: " + Json.stringify((JsonNode) s) + "\n\n";
            } else {
                return "data: " + /**org.apache.commons.lang3.StringEscapeUtils.escapeEcmaScript(*/s.toString() + "\n\n";
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
            public void invoke() throws Throwable {}
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
        return Controller.ok(comet);
    }

    public static <T> Results.Status comet(String callback, final Enumerator<T> enumerator) {
        return comet(callback, enumerator, new StrBuilder<T>() {
            @Override
            public String build(T value) {
                if (value instanceof JsonNode) {
                    return Json.stringify((JsonNode) value);
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
                    return Json.stringify((JsonNode) value);
                } else {
                    return value.toString();
                }
            }
        });
    }

    public static <T> WebSocket<T> websocket(final Class<T> clazz, final Iteratee<T, Unit> inIteratee, final Enumerator<T> outEnumerator) {
        return websocket(clazz, clazz, inIteratee, new Identity<T>(), outEnumerator, new Identity<T>());
    }

    public static <T> WebSocket<T> websocket(final Class<T> clazz, final Iteratee<T, Unit> inIteratee, final HubEnumerator<T> outEnumerator) {
        return websocket(clazz, clazz, inIteratee, new Identity<T>(), outEnumerator, new Identity<T>());
    }

    public static <T> WebSocket<String> websocketStr(final Iteratee<String, Unit> inIteratee, final Enumerator<String> outEnumerator) {
        return websocket(String.class, String.class, inIteratee, new Identity<String>(), outEnumerator, new Identity<String>());
    }

    public static WebSocket<String> websocketStr(final Iteratee<String, Unit> inIteratee, final HubEnumerator<String> outEnumerator) {
        return websocket(String.class, String.class, inIteratee, new Identity<String>(), outEnumerator, new Identity<String>());
    }

    public static <T> WebSocket<JsonNode> websocketJSON(final Iteratee<JsonNode, Unit> inIteratee, final Enumerator<JsonNode> outEnumerator) {
        return websocket(JsonNode.class, JsonNode.class, inIteratee, new Identity<JsonNode>(), outEnumerator, new Identity<JsonNode>());
    }

    public static WebSocket<JsonNode> websocketJSON(final Iteratee<JsonNode, Unit> inIteratee, final HubEnumerator<JsonNode> outEnumerator) {
        return websocket(JsonNode.class, JsonNode.class, inIteratee, new Identity<JsonNode>(), outEnumerator, new Identity<JsonNode>());
    }

    public static <IO, FR> WebSocket<IO> websocket(final Class<IO> clazz, final Class<FR> from,
            final Iteratee<FR, Unit> inIteratee, final Builder<IO, FR> inBuilder,
            final Enumerator<FR> outEnumerator, final Builder<FR, IO> outBuilder) {

        WebSocket<IO> ws =  new WebSocket<IO>() {
            public void onReady(final WebSocket.In<IO> in, final WebSocket.Out<IO> out) {
                final Iteratee<FR, Unit> send = Iteratee.foreach(new Function<FR, Unit>() {
                    @Override
                    public Unit apply(FR s) {
                        out.write(outBuilder.build(s));
                        return Unit.unit();
                    }
                });
                final PushEnumerator<IO> push = Enumerator.unicast(clazz);
                in.onMessage(new play.libs.F.Callback<IO>() {
                    public void invoke(IO event) {
                        push.push(event);
                    }
                });
                in.onClose(new play.libs.F.Callback0() {
                    public void invoke() {
                        push.stop();
                    }
                });
                push.through(Enumeratee.map(new Function<IO, FR>() {
                    @Override
                    public FR apply(IO s) {
                        return inBuilder.build(s);
                    }
                })).applyOn(inIteratee);
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

    public static <IO, FR> WebSocket<IO> websocket(final Class<IO> clazz, final Class<FR> from,
            final Iteratee<FR, Unit> inIteratee, final Builder<IO, FR> inBuilder,
            final HubEnumerator<FR> outEnumerator, final Builder<FR, IO> outBuilder) {

        WebSocket<IO> ws = new WebSocket<IO>() {
            public void onReady(final WebSocket.In<IO> in, final WebSocket.Out<IO> out) {
                final Iteratee<FR, Unit> send = Iteratee.foreach(new Function<FR, Unit>() {
                    @Override
                    public Unit apply(FR s) {
                        out.write(outBuilder.build(s));
                        return Unit.unit();
                    }
                });
                final PushEnumerator<IO> push = Enumerator.unicast(clazz);
                in.onMessage(new play.libs.F.Callback<IO>() {
                    public void invoke(IO event) {
                        push.push(event);
                    }
                });
                in.onClose(new play.libs.F.Callback0() {
                    public void invoke() {
                        push.stop();
                    }
                });
                push.through(Enumeratee.map(new Function<IO, FR>() {
                    @Override
                    public FR apply(IO s) {
                        return inBuilder.build(s);
                    }
                })).applyOn(inIteratee);
                outEnumerator.add(send);
            }
        };
        return ws;
    }
}
