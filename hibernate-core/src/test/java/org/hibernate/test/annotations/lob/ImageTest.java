/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.lob;

import java.util.Arrays;

import org.hibernate.Session;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;
import junit.framework.AssertionFailedError;

/**
 * Tests eager materialization and mutation of data mapped by
 * {@link org.hibernate.type.ImageType}.
 *
 * @author Gail Badner
 */
@RequiresDialect( { SQLServerDialect.class, SybaseDialect.class })
public class ImageTest extends BaseCoreFunctionalTestCase {
	private static final int ARRAY_SIZE = 10000;

	@Test
	public void testBoundedLongByteArrayAccess() {
		byte[] original = buildRecursively(ARRAY_SIZE, true);
		byte[] changed = buildRecursively(ARRAY_SIZE, false);

		Session s = openSession();
		s.beginTransaction();
		ImageHolder entity = new ImageHolder();
		s.save(entity);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = (ImageHolder) s.get(ImageHolder.class, entity.getId());
		Assert.assertNull( entity.getLongByteArray() );
		Assert.assertNull( entity.getDog() );
		Assert.assertNull( entity.getPicByteArray() );
		entity.setLongByteArray(original);
		Dog dog = new Dog();
		dog.setName("rabbit");
		entity.setDog(dog);
		entity.setPicByteArray(wrapPrimitive(original));
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = (ImageHolder) s.get(ImageHolder.class, entity.getId());
		Assert.assertEquals( ARRAY_SIZE, entity.getLongByteArray().length );
		assertEquals(original, entity.getLongByteArray());
		Assert.assertEquals( ARRAY_SIZE, entity.getPicByteArray().length );
		assertEquals(original, unwrapNonPrimitive(entity.getPicByteArray()));
		Assert.assertNotNull( entity.getDog() );
		Assert.assertEquals( dog.getName(), entity.getDog().getName() );
		entity.setLongByteArray(changed);
		entity.setPicByteArray(wrapPrimitive(changed));
		dog.setName("papa");
		entity.setDog(dog);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = (ImageHolder) s.get(ImageHolder.class, entity.getId());
		Assert.assertEquals( ARRAY_SIZE, entity.getLongByteArray().length );
		assertEquals(changed, entity.getLongByteArray());
		Assert.assertEquals( ARRAY_SIZE, entity.getPicByteArray().length );
		assertEquals(changed, unwrapNonPrimitive(entity.getPicByteArray()));
		Assert.assertNotNull( entity.getDog() );
		Assert.assertEquals( dog.getName(), entity.getDog().getName() );
		entity.setLongByteArray(null);
		entity.setPicByteArray(null);
		entity.setDog(null);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = (ImageHolder) s.get(ImageHolder.class, entity.getId());
		Assert.assertNull( entity.getLongByteArray() );
		Assert.assertNull( entity.getDog() );
		Assert.assertNull( entity.getPicByteArray() );
		s.delete(entity);
		s.getTransaction().commit();
		s.close();
	}

	private Byte[] wrapPrimitive(byte[] bytes) {
		int length = bytes.length;
		Byte[] result = new Byte[length];
		for (int index = 0; index < length; index++) {
			result[index] = Byte.valueOf( bytes[index] );
		}
		return result;
	}

	private byte[] unwrapNonPrimitive(Byte[] bytes) {
		int length = bytes.length;
		byte[] result = new byte[length];
		for (int i = 0; i < length; i++) {
			result[i] = bytes[i].byteValue();
		}
		return result;
	}

	private byte[] buildRecursively(int size, boolean on) {
		byte[] data = new byte[size];
		data[0] = mask(on);
		for (int i = 0; i < size; i++) {
			data[i] = mask(on);
			on = !on;
		}
		return data;
	}

	private byte mask(boolean on) {
		return on ? (byte) 1 : (byte) 0;
	}

	public static void assertEquals(byte[] val1, byte[] val2) {
		if ( !Arrays.equals( val1, val2 ) ) {
			throw new AssertionFailedError("byte arrays did not match");
		}
	}

	@Override
	protected String[] getAnnotatedPackages() {
		return new String[] { "org.hibernate.test.annotations.lob" };
	}

	@Override
    public Class<?>[] getAnnotatedClasses() {
		return new Class[] { ImageHolder.class };
	}

}
