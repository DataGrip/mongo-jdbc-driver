package com.dbschema.mongo.nashorn;

import jdk.nashorn.api.scripting.AbstractJSObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Liudmila Kornilova
 **/
public class MongoJSObject extends AbstractJSObject {
  private final Map<String, List<MemberFunction>> membersMap = new HashMap<>();

  public MongoJSObject(@NotNull List<MemberFunction> members) {
    for (MemberFunction member : members) {
      this.membersMap.computeIfAbsent(member.getName(), s -> new ArrayList<>()).add(member);
    }
  }

  @Override
  public boolean hasMember(String name) {
    return membersMap.containsKey(name);
  }

  @Nullable
  @Override
  public AbstractJSObject getMember(String name) {
    List<MemberFunction> members = membersMap.get(name);
    if (members == null || members.isEmpty()) return null;
    return new AbstractJSObject() {
      @Override
      public Object call(Object thiz, Object... args) {
        for (MemberFunction member : members) {
          Optional<Object> result = member.tryRun(args);
          if (result.isPresent()) return result.get();
        }
        throw new UnsupportedOperationException("Unable to find function with name " + name + " and arguments " + Arrays.toString(args));
      }

      @Override
      public boolean isFunction() {
        return true;
      }
    };
  }
}
