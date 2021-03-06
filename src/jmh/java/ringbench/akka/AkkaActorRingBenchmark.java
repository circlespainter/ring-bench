package ringbench.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import co.paralleluniverse.fibers.SuspendExecution;
import org.openjdk.jmh.infra.Blackhole;
import ringbench.AbstractRingBenchmark;
import ringbench.RingWorker;

import java.util.concurrent.CountDownLatch;

/**
 * Ring benchmark using Akka actors.
 *
 * Internally actors use a {@link java.util.concurrent.CountDownLatch} to
 * notify the completion of the ring.
 */
public class AkkaActorRingBenchmark extends AbstractRingBenchmark<ActorRef> {
    private ActorSystem system;

    protected static class InternalActor extends UntypedActor implements RingWorker {
        protected final int id;
        protected final int[] sequences;
        protected final CountDownLatch latch;
        private final Blackhole blackHole;
        protected ActorRef next = null;

        public InternalActor(final int id, final int[] sequences, final CountDownLatch latch, final Blackhole bh) {
            this.id = id;
            this.sequences = sequences;
            this.latch = latch;
            this.blackHole = bh;
        }

        @Override public void onReceive(final Object message) throws Exception {
            if (message instanceof Integer) {
                final int sequence = (Integer) message;
                if (sequence < 1) {
                    sequences[id] = sequence;
                    latch.countDown();
                    getContext().stop(getSelf());
                }
                doWork(blackHole);
                next.tell(sequence - 1, getSelf());
            }
            else if (message instanceof ActorRef) next = (ActorRef) message;
            else unhandled(message);
        }
    }

    @Override
    protected ActorRef[][] setupWorkers(final int[][] sequences, final CountDownLatch cdl, final Blackhole bh) {
        system = ActorSystem.create(AkkaActorRingBenchmark.class.getSimpleName() + "System");

        final ActorRef[][] actors = new ActorRef[sequences.length][sequences.length >= 0 ? sequences[0].length : 0];

        for(int i = 0; i < rings; i++) {
            final ActorRef[] acts = actors[i];
            final int len = acts.length;

            for (int j = 0; j < len; j++)
                acts[j] = system.actorOf(
                        Props.create(InternalActor.class, j, sequences[i], cdl, bh),
                        String.format("%s-%d-%d", AkkaActorRingBenchmark.class.getSimpleName(), i, j));

            // Set next actor pointers.
            for (int j = 0; j < len; j++)
                actors[i][j].tell(actors[i][(j + 1) % len], null);
        }

        return actors;
    }

    @Override
    protected void startRing(final ActorRef first) throws SuspendExecution {
        first.tell(ringSize, null);
    }

    @Override
    protected void shutdown() {
        system.shutdown();
    }
}
