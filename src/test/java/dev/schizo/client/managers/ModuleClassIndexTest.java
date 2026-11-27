package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModuleClassIndexTest {
   @Test
   void rejectedRegistrationDoesNotPopulateClassLookup() {
      Map<Class<?>, TestModule> index = new HashMap<>();
      TestModule rejected = new TestModule();

      assertFalse(ModuleClassIndex.cacheSuccessfulRegistration(index, rejected, false));
      assertNull(index.get(TestModule.class));
   }

   @Test
   void successfulRegistrationPopulatesClassLookup() {
      Map<Class<?>, TestModule> index = new HashMap<>();
      TestModule registered = new TestModule();

      assertTrue(ModuleClassIndex.cacheSuccessfulRegistration(index, registered, true));
      assertSame(registered, index.get(TestModule.class));
   }

   @Test
   void rejectedDuplicateDoesNotReplaceTheExistingClassEntry() {
      Map<Class<?>, TestModule> index = new HashMap<>();
      TestModule existing = new TestModule();
      TestModule duplicate = new TestModule();
      index.put(TestModule.class, existing);

      assertFalse(ModuleClassIndex.cacheSuccessfulRegistration(index, duplicate, false));
      assertSame(existing, index.get(TestModule.class));
   }

   @Test
   void unregisterRemovesTheRegistryEntryAndMatchingCacheValue() {
      Map<Class<?>, TestModule> index = new HashMap<>();
      List<TestModule> entries = new ArrayList<>();
      TestModule registered = new TestModule();
      entries.add(registered);
      index.put(TestModule.class, registered);

      assertTrue(ModuleClassIndex.unregister(entries, index, registered));
      assertTrue(entries.isEmpty());
      assertNull(index.get(TestModule.class));
   }

   @Test
   void unregisteringAnAbsentInstanceLeavesTheExistingCacheEntryUntouched() {
      Map<Class<?>, TestModule> index = new HashMap<>();
      List<TestModule> entries = new ArrayList<>();
      TestModule registered = new TestModule();
      index.put(TestModule.class, registered);

      assertFalse(ModuleClassIndex.unregister(entries, index, registered));
      assertSame(registered, index.get(TestModule.class));
   }

   private static final class TestModule {
   }
}
