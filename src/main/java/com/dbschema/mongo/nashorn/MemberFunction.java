package com.dbschema.mongo.nashorn;

import jdk.nashorn.internal.runtime.ScriptRuntime;

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
        return Optional.of(ScriptRuntime.UNDEFINED);
      }
      return Optional.empty();
    });
  }

  public static <R> MemberFunction func(String name, Supplier<R> function) {
    return new MemberFunction(name, args -> {
      if (args.length == 0) {
        return Optional.of(function.get());
      }
      return Optional.empty();
    });
  }

  public static <T, R> MemberFunction func(String name, Function<T, R> function, Class<T> clazz) {
    return new MemberFunction(name, args -> {
      if (args.length == 1 && clazz.isInstance(args[0])) {
        //noinspection unchecked
        return Optional.of(function.apply((T) args[0]));
      }
      return Optional.empty();
    });
  }

  public static <T1, T2, R> MemberFunction func(String name, BiFunction<T1, T2, R> function, Class<T1> clazzA, Class<T2> clazzB) {
    return new MemberFunction(name, args -> {
      if (args.length == 2 && clazzA.isInstance(args[0]) && clazzB.isInstance(args[1])) {
        //noinspection unchecked
        return Optional.of(function.apply((T1) args[0], (T2) args[1]));
      }
      return Optional.empty();
    });
  }

  public static <T1, T2, T3, R> MemberFunction func(String name, TriFunction<T1, T2, T3, R> function, Class<T1> clazzA, Class<T2> clazzB, Class<T3> classC) {
    return new MemberFunction(name, args -> {
      if (args.length == 3 && clazzA.isInstance(args[0]) && clazzB.isInstance(args[1]) && classC.isInstance(args[2])) {
        //noinspection unchecked
        return Optional.of(function.apply((T1) args[0], (T2) args[1], (T3) args[2]));
      }
      return Optional.empty();
    });
  }

  public static <T> MemberFunction voidFunc(String name, Consumer<T> function, Class<T> clazz) {
    return new MemberFunction(name, args -> {
      if (args.length == 1 && clazz.isInstance(args[0])) {
        //noinspection unchecked
        function.accept((T) args[0]);
        return Optional.of(ScriptRuntime.UNDEFINED);
      }
      return Optional.empty();
    });
  }

  public static <T1, T2> MemberFunction voidFunc(String name, BiConsumer<T1, T2> function, Class<T1> clazzA, Class<T2> classB) {
    return new MemberFunction(name, args -> {
      if (args.length == 2 && clazzA.isInstance(args[0]) && classB.isInstance(args[1])) {
        //noinspection unchecked
        function.accept((T1) args[0], (T2) args[1]);
        return Optional.of(ScriptRuntime.UNDEFINED);
      }
      return Optional.empty();
    });
  }

  public static <T1, T2, T3> MemberFunction voidFunc(String name, TriConsumer<T1, T2, T3> function, Class<T1> clazzA, Class<T2> classB, Class<T3> classC) {
    return new MemberFunction(name, args -> {
      if (args.length == 3 && clazzA.isInstance(args[0]) && classB.isInstance(args[1]) && classC.isInstance(args[2])) {
        //noinspection unchecked
        function.accept((T1) args[0], (T2) args[1], (T3) args[2]);
        return Optional.of(ScriptRuntime.UNDEFINED);
      }
      return Optional.empty();
    });
  }
}
