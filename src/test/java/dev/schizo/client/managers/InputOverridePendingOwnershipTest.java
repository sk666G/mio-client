package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class InputOverridePendingOwnershipTest {
   @Test
   void onlyTheOwningRequesterCanClearAPendingValue() throws Exception {
      Class<?> pendingType = Class.forName(
         "dev.schizo.client.managers.InputOverrideManager$PendingValue"
      );
      Constructor<?> constructor = pendingType.getDeclaredConstructor();
      constructor.setAccessible(true);
      Object pending = constructor.newInstance();

      Method setOwned = pendingType.getDeclaredMethod("set", Object.class, Object.class);
      Method clearOwned = pendingType.getDeclaredMethod("clear", Object.class);
      Method isPending = pendingType.getSuperclass().getDeclaredMethod("isPending");
      setOwned.setAccessible(true);
      clearOwned.setAccessible(true);
      isPending.setAccessible(true);

      Object owner = new Object();
      Object other = new Object();
      setOwned.invoke(pending, owner, Boolean.TRUE);
      assertTrue((Boolean)isPending.invoke(pending));

      clearOwned.invoke(pending, other);
      assertTrue((Boolean)isPending.invoke(pending));

      clearOwned.invoke(pending, owner);
      assertFalse((Boolean)isPending.invoke(pending));
   }
}
