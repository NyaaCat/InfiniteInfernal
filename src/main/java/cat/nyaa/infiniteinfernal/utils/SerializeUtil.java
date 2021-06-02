package cat.nyaa.infiniteinfernal.utils;

import cat.nyaa.nyaacore.configuration.ISerializable;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Stream;

public class SerializeUtil {
    public Stream<Field> getProperties(ISerializable iSerializable){
        Class<?> aClass = iSerializable.getClass();
        Stream<Field> fieldStream = Stream.empty();
        while (aClass != null){
            Field[] declaredFields = aClass.getDeclaredFields();
            //check @ISerializable fields
            Stream<Field> toAppend = Arrays.stream(declaredFields)
                    .filter(field -> field.getAnnotation(ISerializable.Serializable.class) != null);
            fieldStream = Stream.concat(fieldStream, toAppend);

            aClass = aClass.getSuperclass();
        }
    }
}
