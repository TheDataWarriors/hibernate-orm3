/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;


/**
 * @author hbm2java
 */
public class Person extends org.hibernate.test.legacy.Party {

   java.lang.String id;
   java.lang.String givenName;
   java.lang.String lastName;
   java.lang.String nationalID;


   @Override
  java.lang.String getId() {
    return id;
  }

   @Override
  void  setId(java.lang.String newValue) {
    id = newValue;
  }

  java.lang.String getGivenName() {
    return givenName;
  }

  void  setGivenName(java.lang.String newValue) {
    givenName = newValue;
  }

  java.lang.String getLastName() {
    return lastName;
  }

  void  setLastName(java.lang.String newValue) {
    lastName = newValue;
  }

  java.lang.String getNationalID() {
    return nationalID;
  }

  void  setNationalID(java.lang.String newValue) {
    nationalID = newValue;
  }


}
