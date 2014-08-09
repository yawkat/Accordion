package at.yawk.accordion.codec.unsafe;

import java.lang.reflect.Field;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
interface FieldWrapper {
    Class<?> type();

    Optional<FieldWrapper> genericType(int index);

    String name();

    public static FieldWrapper field(Field field) {
        return new FieldFieldWrapper(field);
    }

    public static FieldWrapper clazz(Class<?> clazz) {
        return new ClassFieldWrapper(clazz);
    }

    @EqualsAndHashCode
    @RequiredArgsConstructor
    class FieldFieldWrapper implements FieldWrapper {
        private final Field field;

        @Override
        public Class<?> type() {
            return field.getType();
        }

        @Override
        public Optional<FieldWrapper> genericType(int index) {
            return Optional.ofNullable(TypeParameters.getTypeParameter(field, index)).map(FieldWrapper::clazz);
        }

        @Override
        public String name() {
            return field.getDeclaringClass().getName() + "#" + field.getName();
        }
    }

    @EqualsAndHashCode
    @RequiredArgsConstructor
    class ClassFieldWrapper implements FieldWrapper {
        private final Class<?> clazz;

        @Override
        public Class<?> type() {
            return clazz;
        }

        @Override
        public Optional<FieldWrapper> genericType(int index) {
            return Optional.empty();
        }

        @Override
        public String name() {
            return clazz.getName();
        }
    }
}
