/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.bytecode.enhancement;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.cfg.Environment;
import org.junit.internal.builders.JUnit4Builder;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Luis Barreiro
 */
public class BytecodeEnhancerRunner extends Suite {

	public BytecodeEnhancerRunner(Class<?> klass) throws ClassNotFoundException, InitializationError {
		super( new JUnit4Builder(), klass, enhanceTestClass( klass ) );
	}

	private static Class<?>[] enhanceTestClass(Class<?> klass) throws ClassNotFoundException {
		String packageName = klass.getPackage().getName();
		List<Class<?>> classList = new ArrayList<>();

		try {
			if ( klass.isAnnotationPresent( CustomEnhancementContext.class ) ) {
				for ( Class<? extends EnhancementContext> contextClass : klass.getAnnotation( CustomEnhancementContext.class ).value() ) {
					EnhancementContext enhancementContextInstance = contextClass.getConstructor().newInstance();
					classList.add( getEnhancerClassLoader( enhancementContextInstance, packageName ).loadClass( klass.getName() ) );
				}
			}
			else {
				classList.add( getEnhancerClassLoader( new EnhancerTestContext(), packageName ).loadClass( klass.getName() ) );
			}
		}
		catch ( IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e ) {
			// This is unlikely, but if happens throw runtime exception to fail the test
			throw new RuntimeException( e );
		}
		return classList.toArray( new Class<?>[]{} );
	}

	// --- //

	private static ClassLoader getEnhancerClassLoader(EnhancementContext context, String packageName) {
		return new ClassLoader() {

			private final String debugOutputDir = System.getProperty( "java.io.tmpdir" );

			private final Enhancer enhancer = Environment.getBytecodeProvider().getEnhancer( context );

			@SuppressWarnings( "ResultOfMethodCallIgnored" )
			@Override
			public Class<?> loadClass(String name) throws ClassNotFoundException {
				if ( !name.startsWith( packageName ) ) {
					return getParent().loadClass( name );
				}
				Class c = findLoadedClass( name );
				if ( c != null ) {
					return c;
				}

				try ( InputStream is = getResourceAsStream( name.replace( '.', '/' ) + ".class" ) ) {
					if ( is == null ) {
						throw new ClassNotFoundException( name + " not found" );
					}

					byte[] original = new byte[is.available()];
					try ( BufferedInputStream bis = new BufferedInputStream( is ) ) {
						bis.read( original );
					}

					byte[] enhanced = enhancer.enhance( name, original );
					if ( enhanced == null ) {
						return defineClass( name, original, 0, original.length );
					}

					File f = new File( debugOutputDir + File.separator + name.replace( ".", File.separator ) + ".class" );
					f.getParentFile().mkdirs();
					f.createNewFile();
					try ( FileOutputStream out = new FileOutputStream( f ) ) {
						out.write( enhanced );
					}
					return defineClass( name, enhanced, 0, enhanced.length );
				}
				catch ( Throwable t ) {
					throw new ClassNotFoundException( name + " not found", t );
				}
			}
		};
	}

}
