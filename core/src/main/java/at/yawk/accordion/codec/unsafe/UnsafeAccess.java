/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.codec.unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import sun.misc.Unsafe;

/**
 * @author yawkat
 */
class UnsafeAccess {
    static final Unsafe unsafe;

    static {
        Unsafe theUnsafe;
        try {
            theUnsafe = loadUnsafeFromField();
        } catch (ReflectiveOperationException e) {
            try {
                theUnsafe = createNewUnsafe();
            } catch (ReflectiveOperationException f) {
                throw new Error(f);
            }
        }
        unsafe = theUnsafe;
    }

    private static Unsafe loadUnsafeFromField() throws ReflectiveOperationException {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    private static Unsafe createNewUnsafe() throws ReflectiveOperationException {
        Constructor<Unsafe> constructor = Unsafe.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
