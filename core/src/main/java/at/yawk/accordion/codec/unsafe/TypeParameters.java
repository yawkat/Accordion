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
