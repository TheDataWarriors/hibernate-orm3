/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.hibernate.shards.strategy.exit;

import org.hibernate.criterion.Order;
import org.hibernate.shards.util.Preconditions;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Maulik Shah
 */
public class OrderExitOperation implements ExitOperation {

    private final Order order;
    private final String propertyName;

    public OrderExitOperation(final Order order) {
        Preconditions.checkState(order.toString().toLowerCase().endsWith("asc") ||
                order.toString().toLowerCase().endsWith("desc"));

        this.order = order;
        this.propertyName = getSortingProperty(order);
    }

    public List<Object> apply(final List<Object> results) {
        final List<Object> nonNullList = ExitOperationUtils.getNonNullList(results);
        final Comparator<Object> comparator = new Comparator<Object>() {
            public int compare(Object o1, Object o2) {
                if (o1 == o2) {
                    return 0;
                }
                Comparable<Object> o1Value = ExitOperationUtils.getPropertyValue(o1, propertyName);
                Comparable<Object> o2Value = ExitOperationUtils.getPropertyValue(o2, propertyName);
                if (o1Value == null) {
                    return -1;
                }
                return o1Value.compareTo(o2Value);
            }
        };

        Collections.sort(nonNullList, comparator);
        if (order.toString().endsWith("desc")) {
            Collections.reverse(nonNullList);
        }

        return nonNullList;
    }

    public static String getSortingProperty(final Order order) {
        /**
         * This method relies on the format that Order is using:
         * propertyName + ' ' + (ascending?"asc":"desc")
         */
        final String str = order.toString();
        return str.substring(0, str.indexOf(' '));
    }
}
