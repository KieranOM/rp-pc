package rp.robotics.testing;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.time.Instant;

import rp.robotics.MobileRobot;
import rp.robotics.simulation.MapBasedSimulation;
import rp.robotics.simulation.SimulationSteppable;
import rp.systems.StoppableRunnable;

/**
 * 
 * Test that checks whether the robot maintains range readings under a given
 * limit for a set time.
 * 
 * @author Nick Hawes
 *
 */
public abstract class DistanceLimitTest<C extends StoppableRunnable> extends
		RobotTest<C> {

	private final float m_rangeLimit;
	private final Duration m_allowableOutsideLimit;
	private final Duration m_startupTime;
	private boolean m_failed = false;
	private AssertionError m_error;

	public DistanceLimitTest(MapBasedSimulation _sim, float _limit,
			C _controller, MobileRobot _poser, Duration _timeout,
			Duration _allowableOutsideLimit, Duration _startupTime) {
		super(_sim, _controller, _poser, _timeout);
		m_rangeLimit = _limit;
		m_allowableOutsideLimit = _allowableOutsideLimit;
		m_startupTime = _startupTime;

	}

	private boolean isWithinRange(float _reading) {
		return _reading <= m_rangeLimit;
	}

	protected abstract float getDistance();

	@Override
	public void run() {

		assertTrue("Controller could not be created for this test.",
				m_controller != null);

		getSimulation().start();

		Thread t = new Thread(m_controller);
		Instant now = Instant.now();

		Instant endAt = now.plus(m_timeout);
		Instant startAfter = now.plus(m_startupTime);

		assertTrue(startAfter.isBefore(endAt));

		try {
			t.start();
			m_sim.getSimulationCore().addAndWaitSteppable(
					new SimulationSteppable() {

						boolean ended = false;
						Instant lastGood;

						@Override
						public void step(Instant _now, Duration _stepInterval) {

							// System.out.println("now: " + _now);
							// System.out.println("end: " + endAt);
							// System.out.println("after: " + startAfter);
							// System.out.println("last: " + lastGood);

							try {

								if (_now.isAfter(endAt)) {
									ended = true;
									System.out
											.println("Successfully reached end of test");
								} else if (lastGood != null
										&& _now.isAfter(startAfter)) {

									float reading = getDistance();

									// System.out.println(reading);

									if (reading <= 0.03) {
										// System.out.println("FAIL 1");

										fail("Robot is too close to obstacle. Distance: "
												+ reading);
									} else if (isWithinRange(reading)) {
										// System.out.println("Good reading!");
										lastGood = _now;
									} else if (Duration.between(lastGood, _now)
											.compareTo(m_allowableOutsideLimit) > 0) {

										// System.out.println("FAIL 2");

										fail("Distance exceeded limit of "
												+ m_rangeLimit
												+ " for longer than allowable duration of "
												+ m_allowableOutsideLimit);
									}

								} else {
									lastGood = _now;
								}
							} catch (AssertionError e) {
								m_failed = true;
								m_error = e;
							}
						}

						@Override
						public boolean remove(Instant _now,
								Duration _stepInterval) {
							return m_failed || ended;
						}
					});

			if (m_failed) {
				// System.out.println("Rethrowing error on failure.");
				throw m_error;
			}

		} finally {

			long stopCalledAt = System.currentTimeMillis();
			// System.out.println("stopping controller");
			m_controller.stop();
			try {
				// System.out.println("joining");
				t.join(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			callListenersControllerStopped(m_poser, System.currentTimeMillis()
					- stopCalledAt);
			// System.out.println("done");

		}
	}
}
