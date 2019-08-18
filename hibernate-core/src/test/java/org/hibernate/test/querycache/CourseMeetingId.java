/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.querycache;
import java.io.Serializable;

/**
 * @author Gail Badner
 */
public class CourseMeetingId implements Serializable {
	private String courseCode;
	private String day;
	private int period;
	private String location;

	public CourseMeetingId() {}

	public CourseMeetingId(Course course, String day, int period, String location) {
		this.courseCode = course.getCourseCode();
		this.day = day;
		this.period = period;
		this.location = location;
	}

	public String getCourseCode() {
		return courseCode;
	}
	public void setCourseCode(String courseCode) {
		this.courseCode = courseCode;
	}
	public String getDay() {
		return day;
	}
	public void setDay(String day) {
		this.day = day;
	}
	public int getPeriod() {
		return period;
	}
	public void setPeriod(int period) {
		this.period = period;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		CourseMeetingId that = ( CourseMeetingId ) o;

		if ( period != that.period ) {
			return false;
		}
		if ( courseCode != null ? !courseCode.equals( that.courseCode ) : that.courseCode != null ) {
			return false;
		}
		if ( day != null ? !day.equals( that.day ) : that.day != null ) {
			return false;
		}
		if ( location != null ? !location.equals( that.location ) : that.location != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = courseCode != null ? courseCode.hashCode() : 0;
		result = 31 * result + ( day != null ? day.hashCode() : 0 );
		result = 31 * result + period;
		result = 31 * result + ( location != null ? location.hashCode() : 0 );
		return result;
	}
}
