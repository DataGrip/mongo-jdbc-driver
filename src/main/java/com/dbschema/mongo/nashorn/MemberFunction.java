package com.dbschema.mongo.nashorn;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.*;

/**
 * @author Liudmila Kornilova
 **/
public class MemberFunction {
  private final String name;
  private final Function<Object[], Optional<Object>> function;

  public MemberFunction(String name, Function<Object[], Optional<Object>> function) {
    this.name = name;
    this.function = function;
  }

  public Optional<Object> tryRun(Object... args) {
    return function.apply(args);
  }

  public String getName() {
    return name;
  }

  public static MemberFunction voidFunc(String name, Runnable function) {
    return new MemberFunction(name, args -> {
      if (args.length == 0) {
        function.run();
        return Optional.of(Undefined.INSTANCE);
      }
      return Optional.empty();
    });
  }

  public static <R> MemberFunction func(String name, Supplier<R> function) {
    return new MemberFunction(name, args -> args.length == 0 ? Optional.of(function.get()) : Optional.empty());
  }

  public static MemberFunction ignoreParams(String name, Runnable function) {
    return new MemberFunction(name, args -> {
      function.run();
      return Optional.of(Undefined.INSTANCE);
    });
  }

  public static MemberFunction notImplemented(String name, ObjectKind objectKind) {
    return ignoreParams(name, () -> {
      String msg;
      switch (objectKind) {
        case DATABASE:
          msg = "Method db." + name + " is not implemented";
          break;
        case COLLECTION:
          msg = "Method db.collection." + name + " is not implemented";
          break;
        case CURSOR:
          msg = "Method cursor." + name + " is not implemented";
          break;
        default:
          msg = "Method " + name + " is not implemented";
      }
      throw new UnsupportedOperationException(msg);
    });
  }

  public static <T, R> MemberFunction func(String name, Function<T, R> function, Class<T> clazz) {
    return new MemberFunction(name, args -> {
      if (args.length != 1) return Optional.empty();
      Optional<T> o1 = isInstance(clazz, args[0]);
      return o1.map(function);
    });
  }

  public static <T, R> MemberFunction vararg(String name, Function<List<T>, R> function, Class<T> clazz) {
    return new MemberFunction(name, args -> {
      List<T> objects = new ArrayList<>();
      for (Object arg : args) {
        Optional<T> o = isInstance(clazz, arg);
        if (!o.isPresent()) return Optional.empty();
        objects.add(o.get());
      }
      return Optional.of(function.apply(objects));
    });
  }

  public static <T1, T2, R> MemberFunction func(String name, BiFunction<T1, T2, R> function, Class<T1> clazzA, Class<T2> clazzB) {
    return new MemberFunction(name, args -> {
      if (args.length != 2) return Optional.empty();
      Optional<T1> o1 = isInstance(clazzA, args[0]);
      Optional<T2> o2 = isInstance(clazzB, args[1]);
      return !o1.isPresent() || !o2.isPresent() ? Optional.empty() : Optional.of(function.apply(o1.get(), o2.get()));
    });
  }

  public static <T1, T2, T3, R> MemberFunction func(String name, TriFunction<T1, T2, T3, R> function, Class<T1> clazzA, Class<T2> clazzB, Class<T3> clazzC) {
    return new MemberFunction(name, args -> {
      if (args.length != 3) return Optional.empty();
      Optional<T1> o1 = isInstance(clazzA, args[0]);
      Optional<T2> o2 = isInstance(clazzB, args[1]);
      Optional<T3> o3 = isInstance(clazzC, args[2]);
      return !o1.isPresent() || !o2.isPresent() || !o3.isPresent() ? Optional.empty() : Optional.of(function.apply(o1.get(), o2.get(), o3.get()));
    });
  }

  public static <T1, T2, T3, T4, R> MemberFunction func(String name, FourFunction<T1, T2, T3, T4, R> function, Class<T1> clazzA, Class<T2> clazzB, Class<T3> clazzC, Class<T4> clazzD) {
    return new MemberFunction(name, args -> {
      if (args.length != 4) return Optional.empty();
      Optional<T1> o1 = isInstance(clazzA, args[0]);
      Optional<T2> o2 = isInstance(clazzB, args[1]);
      Optional<T3> o3 = isInstance(clazzC, args[2]);
      Optional<T4> o4 = isInstance(clazzD, args[3]);
      return !o1.isPresent() || !o2.isPresent() || !o3.isPresent() || !o4.isPresent() ?
             Optional.empty() :
             Optional.of(function.apply(o1.get(), o2.get(), o3.get(), o4.get()));
    });
  }

  public static <T> MemberFunction voidFunc(String name, Consumer<T> function, Class<T> clazz) {
    return new MemberFunction(name, args -> {
      if (args.length != 1) return Optional.empty();
      Optional<T> o1 = isInstance(clazz, args[0]);
      if (!o1.isPresent()) return Optional.empty();
      function.accept(o1.get());
      return Optional.of(Undefined.INSTANCE);
    });
  }

  public static <T1, T2> MemberFunction voidFunc(String name, BiConsumer<T1, T2> function, Class<T1> clazzA, Class<T2> clazzB) {
    return new MemberFunction(name, args -> {
      if (args.length != 2) return Optional.empty();
      Optional<T1> o1 = isInstance(clazzA, args[0]);
      Optional<T2> o2 = isInstance(clazzB, args[1]);
      if (!o1.isPresent() || !o2.isPresent()) return Optional.empty();
      function.accept(o1.get(), o2.get());
      return Optional.of(Undefined.INSTANCE);
    });
  }

  public static <T1, T2, T3> MemberFunction voidFunc(String name, TriConsumer<T1, T2, T3> function, Class<T1> clazzA, Class<T2> clazzB, Class<T3> clazzC) {
    return new MemberFunction(name, args -> {
      if (args.length != 3) return Optional.empty();
      Optional<T1> o1 = isInstance(clazzA, args[0]);
      Optional<T2> o2 = isInstance(clazzB, args[1]);
      Optional<T3> o3 = isInstance(clazzC, args[2]);
      if (!o1.isPresent() || !o2.isPresent() || !o3.isPresent()) return Optional.empty();
      function.accept(o1.get(), o2.get(), o3.get());
      return Optional.of(Undefined.INSTANCE);
    });
  }

  public static <T1, T2, T3, T4> MemberFunction voidFunc(String name, FourConsumer<T1, T2, T3, T4> function, Class<T1> clazzA, Class<T2> clazzB, Class<T3> clazzC, Class<T4> clazzD) {
    return new MemberFunction(name, args -> {
      if (args.length != 4) return Optional.empty();
      Optional<T1> o1 = isInstance(clazzA, args[0]);
      Optional<T2> o2 = isInstance(clazzB, args[1]);
      Optional<T3> o3 = isInstance(clazzC, args[2]);
      Optional<T4> o4 = isInstance(clazzD, args[3]);
      if (!o1.isPresent() || !o2.isPresent() || !o3.isPresent() || !o4.isPresent()) return Optional.empty();
      function.accept(o1.get(), o2.get(), o3.get(), o4.get());
      return Optional.of(Undefined.INSTANCE);
    });
  }

  private static <T> Optional<T> isInstance(@NotNull Class<T> clazz, @Nullable Object value) {
    if (clazz.isInstance(value)) //noinspection unchecked
      return Optional.of((T) value);
    if (clazz == List.class && value instanceof ScriptObjectMirror && ((ScriptObjectMirror) value).isArray()) {
      //noinspection unchecked
      return Optional.of((T) new JSArray((ScriptObjectMirror) value));
    }
    if (clazz == JSFunction.class && value instanceof ScriptObjectMirror && ((ScriptObjectMirror) value).isFunction())
      return Optional.of((T) new JSFunction((ScriptObjectMirror) value));
    return Optional.empty();
  }

  public enum ObjectKind {
    DATABASE,
    COLLECTION,
    CURSOR
  }
}
