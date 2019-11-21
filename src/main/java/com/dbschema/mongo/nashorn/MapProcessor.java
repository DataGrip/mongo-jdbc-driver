package com.dbschema.mongo.nashorn;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  public static <R, T> MapProcessor<R, T> proc(@NotNull Object key, @NotNull Class<T> clazz, @NotNull BiFunction<T, R, R> processor) {
    return new MapProcessor<>(key, clazz, processor);
  }

  Optional<R> processAttribute(@Nullable Object value, @NotNull R object) {
    if (clazz.isInstance(value)) {
      //noinspection unchecked
      return Optional.of(processor.apply((T) value, object));
    }
    return Optional.empty();
  }

  @NotNull
  public Object getKey() {
    return key;
  }

  public static <R> R runProcessors(@NotNull R object, @NotNull List<MapProcessor<R, ?>> processors, Map<?, ?> map) {
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      boolean found = false;
      for (MapProcessor<R, ?> processor : processors) {
        if (!processor.getKey().equals(entry.getKey())) continue;
        Optional<R> newObject = processor.processAttribute(entry.getValue(), object);
        if (newObject.isPresent()) {
          found = true;
          object = newObject.get();
          break;
        }
      }
      if (!found) throw new IllegalArgumentException("Unknown attribute {" + entry.getKey() + ": " + entry.getValue() + "}");
    }
    return object;
  }
}
