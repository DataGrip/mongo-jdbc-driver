package com.dbschema.mongo.nashorn;

import com.dbschema.mongo.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * @author Liudmila Kornilova
 **/
public class MapProcessor<R, T> {
  private final Object key;
  private final BiFunction<T, R, R> processor;
  private Class<T> clazz;

  public MapProcessor(@NotNull Object key, @NotNull Class<T> clazz, @NotNull BiFunction<T, R, R> processor) {
    this.key = key;
    this.processor = processor;
    this.clazz = clazz;
  }

  R processAttribute(@Nullable Object value, @NotNull R object) {
    if (clazz.isInstance(value)) {
      try {
        //noinspection unchecked
        return processor.apply((T) value, object);
      }
      catch (Throwable t) {
        t.printStackTrace();
      }
    }
    return object;
  }

  @NotNull
  public Object getKey() {
    return key;
  }

  public static <R> R runProcessors(@NotNull R object, @NotNull List<MapProcessor<R, ?>> processors, Map<?, ?> map) {
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      MapProcessor<R, ?> processor = Util.find(processors, p -> p.getKey().equals(entry.getKey()));
      if (processor != null) object = processor.processAttribute(entry.getValue(), object);
    }
    return object;
  }
}
