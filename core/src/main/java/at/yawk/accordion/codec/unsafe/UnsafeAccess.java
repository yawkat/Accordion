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
            theUnsafe = UnsafeAccess.loadUnsafeFromField();
        } catch (ReflectiveOperationException e) {
            try {
                theUnsafe = UnsafeAccess.createNewUnsafe();
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
