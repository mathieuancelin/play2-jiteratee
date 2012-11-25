package models;

import iteratee.F;
import static iteratee.F.*;
import iteratee.Iteratees;
import static iteratee.Iteratees.*;

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
    }

    public static class SystemStatus implements Event {
        public String message;
        public SystemStatus(String message) {
            this.message = message;
        }
    }

    public static final Random random = new Random();

    public static final Enumerator<Event> operations = Enumerator.generate(1, TimeUnit.SECONDS, new Function<Unit, Option<Event>>() {
        @Override
        public Option<Event> apply(Unit unit) {
            String status = random.nextBoolean() ? "public" : "private";
            Event evt = new Operation(status, random.nextInt(1000));
            return Option.some(evt);
        }
    });

    public static final Enumerator<Event> noise = Enumerator.generate(5, TimeUnit.SECONDS, new Function<Unit, Option<Event>>() {
        @Override
        public Option<Event> apply(Unit unit) {
            Event evt = new SystemStatus("System message");
            return Option.some(evt);
        }
    });
    public static final Enumerator<Event> events = Enumerator.interleave(operations, noise);

}
