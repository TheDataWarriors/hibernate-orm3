package org.hibernate.test.cache.hhh13179;

public abstract class DiscriminatorSubclassPerson {

   private Long oid;

   public Long getOid() {
      return oid;
   }

   public void setOid(Long oid) {
      this.oid = oid;
   }
}
