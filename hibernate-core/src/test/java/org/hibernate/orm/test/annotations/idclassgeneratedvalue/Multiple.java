/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.idclassgeneratedvalue;
import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.SequenceGenerator;

import org.hibernate.annotations.GenericGenerator;

/**
 * An Entity containing a composite key with two generated values.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Stale W. Pedersen</a>
 */
@Entity
@IdClass(MultiplePK.class)
@SuppressWarnings("serial")
public class Multiple implements Serializable
{
   @Id
   @GenericGenerator(name = "increment", strategy = "increment")
   @GeneratedValue(generator = "increment")
   private Long id1;
   
   @Id
   @GeneratedValue(generator = "MULTIPLE_SEQ", strategy = GenerationType.SEQUENCE)
   @SequenceGenerator( name = "MULTIPLE_SEQ", sequenceName = "MULTIPLE_SEQ")
   private Long id2;
   
   @Id
   private Long id3;
   private int quantity;
   
   public Multiple()
   {
      
   }
   
   public Multiple(Long id3, int quantity)
   {
      this.id3 = id3;
      this.quantity = quantity;
   }

   public Long getId1()
   {
      return id1;
   }

   public Long getId2()
   {
      return id2;
   }

   public Long getId3()
   {
      return id3;
   }

   public int getQuantity()
   {
      return quantity;
   }

   public void setQuantity(int quantity)
   {
      this.quantity = quantity;
   }
   
   
}
