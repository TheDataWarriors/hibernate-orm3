/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.BasicJdbcLiteralFormatter;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#BIT BIT} handling.
 * <p/>
 * Note that JDBC is very specific about its use of the type BIT to mean a single binary digit, whereas
 * SQL defines BIT having a parameterized length.
 *
 * @author Steve Ebersole
 */
public class BitTypeDescriptor implements SqlTypeDescriptor {
	public static final BitTypeDescriptor INSTANCE = new BitTypeDescriptor();

	public BitTypeDescriptor() {
	}

	public int getSqlType() {
		return Types.BIT;
	}

	@Override
	public String getFriendlyName() {
		return "BIT";
	}

	@Override
	public String toString() {
		return "BitTypeDescriptor";
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	@Override
	public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
		return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Boolean.class );
	}

	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
		if ( javaTypeDescriptor.getJavaType().equals(Boolean.class) ) {
			//this is to allow literals to be formatted correctly when
			//we are in the legacy Boolean-to-BIT JDBC type mapping mode
			return new BasicJdbcLiteralFormatter( javaTypeDescriptor ) {
				@Override
				public String toJdbcLiteral(Object value, Dialect dialect, SharedSessionContractImplementor session) {
					Boolean bool = unwrap( value, Boolean.class, session );
					return bool ? "1" : "0";
				}
			};
		}
		else {
			return (value, dialect, session) -> value.toString();
		}
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				st.setBoolean( index, javaTypeDescriptor.unwrap( value, Boolean.class, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setBoolean( name, javaTypeDescriptor.unwrap( value, Boolean.class, options ) );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( rs.getBoolean( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getBoolean( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getBoolean( name ), options );
			}
		};
	}
}
