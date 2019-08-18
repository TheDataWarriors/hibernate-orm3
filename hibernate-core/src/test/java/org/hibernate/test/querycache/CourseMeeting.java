/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.querycache;


/**
 * @author Gail Badner
 */
public class CourseMeeting {
	private CourseMeetingId id;
	private Course course;

	public CourseMeeting() {}

	public CourseMeeting(Course course, String day, int period, String location) {
		this.id = new CourseMeetingId( course, day, period, location );
		this.course = course;
	}

	public CourseMeetingId getId() {
		return id;
	}
	public void setId(CourseMeetingId id) {
		this.id = id;
	}
	public  Course getCourse() {
		return course;
	}
	public void setCourse(Course course) {
		this.course = course;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		CourseMeeting that = ( CourseMeeting ) o;

		if ( course != null ? !course.equals( that.course ) : that.course != null ) {
			return false;
		}
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + ( course != null ? course.hashCode() : 0 );
		return result;
	}
}
