package models;

import iteratee.F;
import iteratee.Iteratees;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Streams {

    public static interface Event {}

    public static class Operation implements Event {
        public String level;
        public Integer amount;
        public Operation(String level, Integer amount) {
            this.level = level;
            this.amount = amount;
        }
        @Override
        public String toString() {
            return "{\"type\":\"operation\", \"amount\":" + amount + ", \"visibility\":\"" + level + "\"}";
        }
    }

    public static class SystemStatus implements Event {
        public String message;
        public SystemStatus(String message) {
            this.message = message;
        }
        @Override
        public String toString() {
            return "{\"type\":\"status\", \"message\":\"" + message + "\"}";
        }
    }

    public static final Random random = new Random();

    public static final Iteratees.Enumerator<Event> operations = Iteratees.Enumerator.generate(1, TimeUnit.SECONDS, new F.Function<F.Unit, F.Option<Event>>() {
        @Override
        public F.Option<Event> apply(F.Unit unit) {
            String status = random.nextBoolean() ? "public" : "private";
            Event evt = new Operation(status, random.nextInt(1000));
            System.out.println("generate : " + evt);
            return F.Option.some(evt);
        }
    });

    public static final Iteratees.Enumerator<Event> noise = Iteratees.Enumerator.generate(5, TimeUnit.SECONDS, new F.Function<F.Unit, F.Option<Event>>() {
        @Override
        public F.Option<Event> apply(F.Unit unit) {
            Event evt = new SystemStatus("System message");
            System.out.println("generate : " + evt);
            return F.Option.some(evt);
        }
    });
    public static final Iteratees.Enumerator<Event> events = Iteratees.Enumerator.interleave(operations, noise);

}
