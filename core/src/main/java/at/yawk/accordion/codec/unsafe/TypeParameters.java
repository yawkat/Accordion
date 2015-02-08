/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.codec.unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;

/**
 * @author yawkat
 */
class TypeParameters {
    public static Class<?> getTypeParameter(Field of, int i) {
        return (Class<?>) ((ParameterizedType) of.getGenericType()).getActualTypeArguments()[i];
    }
}
