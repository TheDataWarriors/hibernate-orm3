package org.hibernate.orm.test.annotations.refcolnames.embedded;

import jakarta.persistence.Embeddable;

@Embeddable
class TownCode extends PostalCode {
    String town;
}