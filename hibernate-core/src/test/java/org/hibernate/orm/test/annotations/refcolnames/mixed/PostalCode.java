package org.hibernate.orm.test.annotations.refcolnames.mixed;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.MappedSuperclass;

@Embeddable @MappedSuperclass
class PostalCode {
    @Column(name="country_code", nullable = false)
    String countryCode;
    @Column(name="zip_code", nullable = false)
    int zipCode;
}