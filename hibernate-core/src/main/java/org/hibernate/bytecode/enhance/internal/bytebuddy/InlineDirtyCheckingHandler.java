/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.util.Collection;
import java.util.Objects;

import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;

import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl.AnnotatedFieldDescription;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;

final class InlineDirtyCheckingHandler implements Implementation, ByteCodeAppender {

	private final Implementation delegate;

	private final TypeDescription managedCtClass;

	private final FieldDescription.InDefinedShape persistentField;

	private final boolean checkLazyUninitialized;

	private InlineDirtyCheckingHandler(Implementation delegate, TypeDescription managedCtClass, FieldDescription.InDefinedShape persistentField, boolean checkLazyUninitialized) {
		this.delegate = delegate;
		this.managedCtClass = managedCtClass;
		this.persistentField = persistentField;
		this.checkLazyUninitialized = checkLazyUninitialized;
	}

	static Implementation wrap(
			TypeDescription managedCtClass,
			ByteBuddyEnhancementContext enhancementContext,
			AnnotatedFieldDescription persistentField,
			Implementation implementation) {
		if ( enhancementContext.doDirtyCheckingInline( managedCtClass ) ) {

			if ( enhancementContext.isCompositeClass( managedCtClass ) ) {
				implementation = Advice.to( CodeTemplates.CompositeDirtyCheckingHandler.class ).wrap( implementation );
			}
			else if ( !persistentField.hasAnnotation( Id.class )
					&& !persistentField.hasAnnotation( EmbeddedId.class )
					&& !( persistentField.getType().asErasure().isAssignableTo( Collection.class )
					&& enhancementContext.isMappedCollection( persistentField ) ) ) {
				implementation = new InlineDirtyCheckingHandler( implementation, managedCtClass,
						persistentField.asDefined(), enhancementContext.isLazyLoadable( persistentField ) );
			}

			if ( enhancementContext.isCompositeClass( persistentField.getType().asErasure() )
					&& persistentField.hasAnnotation( Embedded.class ) ) {

				// HHH-13759 - Call getter on superclass if field is not visible
				// An embedded field won't be visible if declared private in a superclass
				// annotated with @MappedSuperclass
				Advice.WithCustomMapping advice = Advice.withCustomMapping();
				advice = persistentField.isVisibleTo( managedCtClass )
						? advice.bind( CodeTemplates.FieldValue.class, persistentField.getFieldDescription() )
						: advice.bind( CodeTemplates.FieldValue.class, new CodeTemplates.GetterMapping( persistentField.getFieldDescription() ) );

				implementation = advice
						.bind( CodeTemplates.FieldName.class, persistentField.getName() )
						.to( CodeTemplates.CompositeFieldDirtyCheckingHandler.class )
						.wrap( implementation );
			}
		}
		return implementation;
	}

	@Override
	public ByteCodeAppender appender(Target implementationTarget) {
		return new ByteCodeAppender.Compound( this, delegate.appender( implementationTarget ) );
	}

	@Override
	public InstrumentedType prepare(InstrumentedType instrumentedType) {
		return delegate.prepare( instrumentedType );
	}

	@Override
	public Size apply(
			MethodVisitor methodVisitor,
			Context implementationContext,
			MethodDescription instrumentedMethod) {

		// Skip comparison when lazy not loaded
		final Label uninitializedLazy = new Label();

		if ( checkLazyUninitialized ) {
			// No dirty check is required for non initialized lazy fields.
			final Label skipUninitializedCheckWithPersistentAttributeInterceptor = new Label();
			final Label skipUninitializedCheck = new Label();

			// if ( value !== null || value !== false || value !== 0 || ... ) => no need to detect lazy values
			methodVisitor.visitVarInsn( Type.getType( persistentField.getType().asErasure().getDescriptor() ).getOpcode( Opcodes.ILOAD ), 1 );
			if ( persistentField.getType().isPrimitive() ) {
				if ( persistentField.getType().represents( long.class ) ) {
					methodVisitor.visitInsn( Opcodes.LCONST_0 );
					methodVisitor.visitInsn( Opcodes.LCMP );
					methodVisitor.visitJumpInsn( Opcodes.IFNE, skipUninitializedCheck );
				}
				else if ( persistentField.getType().represents( float.class ) ) {
					methodVisitor.visitInsn( Opcodes.FCONST_0 );
					methodVisitor.visitInsn( Opcodes.FCMPL );
					methodVisitor.visitJumpInsn( Opcodes.IFNE, skipUninitializedCheck );
				}
				else if ( persistentField.getType().represents( double.class ) ) {
					methodVisitor.visitInsn( Opcodes.DCONST_0 );
					methodVisitor.visitInsn( Opcodes.DCMPL );
					methodVisitor.visitJumpInsn( Opcodes.IFNE, skipUninitializedCheck );
				}
				else {
					methodVisitor.visitJumpInsn( Opcodes.IFNE, skipUninitializedCheck );
				}
			}
			else {
				methodVisitor.visitJumpInsn( Opcodes.IFNONNULL, skipUninitializedCheck );
			}

			// if ( this.$$_hibernate_getInterceptor() != null )
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
			methodVisitor.visitMethodInsn(
					Opcodes.INVOKEVIRTUAL,
					managedCtClass.getInternalName(),
					EnhancerConstants.INTERCEPTOR_GETTER_NAME,
					Type.getMethodDescriptor( Type.getType( PersistentAttributeInterceptor.class ) ),
					false
			);

			methodVisitor.visitInsn( Opcodes.DUP );

			// if ( this.$$_hibernate_getInterceptor() == null ) => no dirty check
			methodVisitor.visitJumpInsn( Opcodes.IFNULL, skipUninitializedCheckWithPersistentAttributeInterceptor );

			// if ( !this.$$_hibernate_getInterceptor() instanceof LazyAttributeLoadingInterceptor ) => no dirty check
			methodVisitor.visitInsn( Opcodes.DUP );
			methodVisitor.visitTypeInsn( Opcodes.INSTANCEOF, Type.getInternalName( LazyAttributeLoadingInterceptor.class ) );
			methodVisitor.visitJumpInsn( Opcodes.IFEQ, skipUninitializedCheckWithPersistentAttributeInterceptor );
			methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, Type.getInternalName( LazyAttributeLoadingInterceptor.class ) );

			// call interceptor.isAttributeLoaded( fieldName )
			methodVisitor.visitLdcInsn( persistentField.getName() );
			methodVisitor.visitMethodInsn(
					Opcodes.INVOKEVIRTUAL,
					Type.getInternalName( LazyAttributeLoadingInterceptor.class ),
					"isAttributeLoaded",
					Type.getMethodDescriptor(
							Type.BOOLEAN_TYPE,
							Type.getType( String.class )
					),
					false
			);

			// When field is not initialized, comparison is useless
			// if ( interceptor.isAttributeLoaded( fieldName ) ) goto skipUninitializedCheck
			methodVisitor.visitJumpInsn( Opcodes.IFNE, skipUninitializedCheck );

			// Sure that the lazy is not initialized. No need for comparison, directly set dirty
			methodVisitor.visitJumpInsn( Opcodes.GOTO, uninitializedLazy );

			methodVisitor.visitLabel( skipUninitializedCheckWithPersistentAttributeInterceptor );
			if ( implementationContext.getClassFileVersion().isAtLeast( ClassFileVersion.JAVA_V6 ) ) {
				methodVisitor.visitFrame( Opcodes.F_SAME1, 0, null, 1, new Object[]{Type.getInternalName( PersistentAttributeInterceptor.class )} );
			}
			methodVisitor.visitInsn( Opcodes.POP );

			methodVisitor.visitLabel( skipUninitializedCheck );
			if ( implementationContext.getClassFileVersion().isAtLeast( ClassFileVersion.JAVA_V6 ) ) {
				methodVisitor.visitFrame( Opcodes.F_SAME, 0, null, 0, null );
			}
		}
		// if (arg != field) {
		methodVisitor.visitVarInsn( Type.getType( persistentField.getType().asErasure().getDescriptor() ).getOpcode( Opcodes.ILOAD ), 1 );
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		if ( persistentField.getDeclaringType().asErasure().equals( managedCtClass ) ) {
			methodVisitor.visitFieldInsn(
					Opcodes.GETFIELD,
					persistentField.getDeclaringType().asErasure().getInternalName(),
					persistentField.getName(),
					persistentField.getDescriptor()
			);
		}
		else {
			methodVisitor.visitMethodInsn(
					Opcodes.INVOKEVIRTUAL,
					persistentField.getDeclaringType().asErasure().getInternalName(),
					EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + persistentField.getName(),
					Type.getMethodDescriptor( Type.getType( persistentField.getDescriptor() ) ),
					false
			);
		}
		int branchCode;
		if ( persistentField.getType().isPrimitive() ) {
			if ( persistentField.getType().represents( long.class ) ) {
				methodVisitor.visitInsn( Opcodes.LCMP );
			}
			else if ( persistentField.getType().represents( float.class ) ) {
				methodVisitor.visitInsn( Opcodes.FCMPL );
			}
			else if ( persistentField.getType().represents( double.class ) ) {
				methodVisitor.visitInsn( Opcodes.DCMPL );
			}
			else {
				methodVisitor.visitInsn( Opcodes.ISUB );
			}
			branchCode = Opcodes.IFEQ;
		}
		else {
			methodVisitor.visitMethodInsn(
					Opcodes.INVOKESTATIC,
					Type.getInternalName( Objects.class ),
					"deepEquals",
					Type.getMethodDescriptor( Type.getType( boolean.class ), Type.getType( Object.class ), Type.getType( Object.class ) ),
					false
			);
			branchCode = Opcodes.IFNE;
		}
		Label skip = new Label();
		methodVisitor.visitJumpInsn( branchCode, skip );

		methodVisitor.visitLabel( uninitializedLazy );
		if ( implementationContext.getClassFileVersion().isAtLeast( ClassFileVersion.JAVA_V6 ) ) {
			methodVisitor.visitFrame( Opcodes.F_SAME, 0, null, 0, null );
		}

		// this.$$_hibernate_trackChange(fieldName)
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		methodVisitor.visitLdcInsn( persistentField.getName() );
		methodVisitor.visitMethodInsn(
				Opcodes.INVOKEVIRTUAL,
				managedCtClass.getInternalName(),
				EnhancerConstants.TRACKER_CHANGER_NAME,
				Type.getMethodDescriptor( Type.getType( void.class ), Type.getType( String.class ) ),
				false
		);
		// }
		methodVisitor.visitLabel( skip );
		if ( implementationContext.getClassFileVersion().isAtLeast( ClassFileVersion.JAVA_V6 ) ) {
			methodVisitor.visitFrame( Opcodes.F_SAME, 0, null, 0, null );
		}
		return new Size( 1 + 2 * persistentField.getType().asErasure().getStackSize().getSize(), instrumentedMethod.getStackSize() );
	}

	@Override
	public boolean equals(final Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || InlineDirtyCheckingHandler.class != o.getClass() ) {
			return false;
		}
		final InlineDirtyCheckingHandler that = (InlineDirtyCheckingHandler) o;
		return Objects.equals( delegate, that.delegate ) &&
			Objects.equals( managedCtClass, that.managedCtClass ) &&
			Objects.equals( persistentField, that.persistentField );
	}

	@Override
	public int hashCode() {
		return Objects.hash( delegate, managedCtClass, persistentField );
	}
}
