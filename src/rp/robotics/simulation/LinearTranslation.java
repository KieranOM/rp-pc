package rp.robotics.simulation;

import java.time.Duration;
import java.time.Instant;

import lejos.robotics.navigation.Pose;

/**
 * Movable that translates in the x direction back and forth around the starting
 * pose.
 * 
 * @author nah
 *
 */
public class LinearTranslation implements Movable {

	private Pose m_pose;
	private float m_speed;
	private float m_distance;
	private boolean m_remove = false;

	public LinearTranslation(Pose _pose, float _speed, float _distance) {
		m_pose = _pose;
		m_speed = _speed;
		m_distance = _distance;
		setPose(_pose);
	}

	private boolean m_movePositive = true;
	private float m_limit;

	@Override
	public Pose getPose() {
		synchronized (m_pose) {
			return m_pose;
		}
	}

	/**
	 * Move the movable to this pose. Restarts movement from the given pose.
	 */
	@Override
	public void setPose(Pose _pose) {
		synchronized (m_pose) {
			m_pose = _pose;
			m_limit = m_pose.getX() + m_distance;
			m_movePositive = m_limit > m_pose.getX();

		}

	}

	@Override
	public boolean remove() {
		return m_remove;
	}

	@Override
	public void step(Instant _now, Duration _stepInterval) {
		synchronized (m_pose) {

			float durationSecs = _stepInterval.toMillis() / 1000f;
			float moveAmount = m_movePositive ? m_speed * durationSecs
					: -m_speed * durationSecs;
			m_pose.setLocation(m_pose.getX() + moveAmount, m_pose.getY());

			if ((m_movePositive && m_pose.getX() > m_limit)
					|| (!m_movePositive && m_pose.getX() < m_limit)) {
				m_remove = true;
			}

		}

	}

}
