package at.yawk.accordion.codec.unsafe;

import at.yawk.accordion.codec.ByteCodec;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import lombok.Getter;

/**
 * @author yawkat
 */
public class CodecManager implements CodecSupplier {
    @Getter private static final CodecManager defaultManager = new CodecManager();

    private final List<CodecSupplier> codecSuppliers = new ArrayList<>();

    @SuppressWarnings("ConstantConditions")
    CodecManager() {
        if (defaultManager == null) { // this is the default mgr
            addDefaults();
        } else {
            codecSuppliers.addAll(defaultManager.codecSuppliers);
        }
    }

    private void addDefaults() {
        addSupplier(CommonObjectCodec::factory);

        addSupplier(new OptionalCodec());

        addCollectionCodec(LinkedList.class, i -> new LinkedList());
        addCollectionCodec(HashSet.class, HashSet::new);
        addCollectionCodec(ArrayList.class, ArrayList::new);

        //noinspection unchecked
        addEscalatingObjectCodec(HashMap.class, Map.class, t -> new MapCodec(t, HashMap::new));

        addSupplier(ArrayCodec::factory);

        addUnsafeCodec(boolean.class, PrimitiveCodec.BOOLEAN);
        addUnsafeCodec(byte.class, PrimitiveCodec.BYTE);
        addUnsafeCodec(short.class, PrimitiveCodec.SHORT);
        addUnsafeCodec(char.class, PrimitiveCodec.CHAR);
        addUnsafeCodec(int.class, PrimitiveCodec.INT);
        addUnsafeCodec(long.class, PrimitiveCodec.LONG);
        addUnsafeCodec(float.class, PrimitiveCodec.FLOAT);
        addUnsafeCodec(double.class, PrimitiveCodec.DOUBLE);
    }

    @Override
    public Optional<UnsafeCodec> getCodec(CodecSupplier registry, FieldWrapper field) {
        return codecSuppliers.stream()
                .map(child -> child.getCodec(registry, field))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    CodecManager addSupplier(CodecSupplier supplier) {
        codecSuppliers.add(0, supplier);
        return this;
    }

    <T> CodecManager addUnsafeCodec(Class<T> clazz, UnsafeCodec codec) {
        return addSupplier((m, f) -> {
            if (f.type() == clazz) {
                return Optional.of(codec);
            } else {
                return Optional.empty();
            }
        });
    }

    <T> CodecManager addObjectCodec(Class<T> clazz, ByteCodec<T> codec) {
        return addUnsafeCodec(clazz, new UnsafeByteCodec<T>(codec));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    <L extends P, P> CodecManager addEscalatingObjectCodec(Class<L> lowest,
                                                           Class<P> highest,
                                                           Function<Class<? super L>,
                                                                   CodecSupplier> codecSupplierFactory) {
        superTypes(lowest)
                .filter(highest::isAssignableFrom)
                .map(t -> codecSupplierFactory.apply((Class<? super L>) t))
                .forEach(this::addSupplier);
        return this;
    }

    <C extends Collection<?>> CodecManager addCollectionCodec(Class<C> type, IntFunction<C> factory) {
        return addEscalatingObjectCodec(type,
                                        Collection.class,
                                        t -> makeCollectionCodec(t, factory));
    }

    CodecManager copy() {
        CodecManager manager = new CodecManager();
        manager.codecSuppliers.addAll(this.codecSuppliers);
        return manager;
    }

    @SuppressWarnings("unchecked")
    private static <C> CodecSupplier makeCollectionCodec(Class<C> type, IntFunction<? extends C> factory) {
        return new CollectionCodecSupplier(type, factory);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T> Stream<Class<? super T>> superTypes(Class<T> of) {
        return (Stream) superTypes0(of).distinct();
    }

    private static Stream<Class<?>> superTypes0(Class<?> of) {
        if (of == null) {
            return Stream.empty();
        }
        return Stream.concat(Stream.of(of),
                             Stream.concat(
                                     superTypes0(of.getSuperclass()),
                                     Arrays.stream(of.getInterfaces()).flatMap(CodecManager::superTypes0)
                             ));
    }
}
