package dev.schizo.client.gui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ScissorScopeStackTest {
   private static ScissorBounds bounds(int x, int y, int w, int h) {
      return new ScissorBounds(x, y, w, h);
   }
   private static ScissorScopeStack.Snapshot enabled(int x, int y, int w, int h) {
      return new ScissorScopeStack.Snapshot(true, bounds(x,y,w,h));
   }

   @Test void restoresExternalClipAfterNestedReplacementAndIntersection() {
      var original = enabled(3, 4, 10, 12);
      var state = new AtomicReference<>(original);
      var stack = new ScissorScopeStack(state::get, state::set, b -> b);
      stack.push(bounds(0,0,8,8), true);
      assertEquals(enabled(3,4,5,4), state.get());
      stack.push(bounds(0,0,2,2), false);
      assertEquals(enabled(0,0,2,2), state.get());
      stack.pop();
      assertEquals(enabled(3,4,5,4), state.get());
      stack.pop();
      assertEquals(original, state.get());
      assertThrows(NoSuchElementException.class, stack::pop);
      assertEquals(original, state.get());
   }

   @Test void disabledPredecessorAndDisjointExternalClipArePreserved() {
      var original = new ScissorScopeStack.Snapshot(false, bounds(0,0,0,0));
      var state = new AtomicReference<>(original);
      var stack = new ScissorScopeStack(state::get, state::set, b -> b);
      stack.push(bounds(0,0,4,4), false);
      stack.push(bounds(10,10,4,4), true);
      assertTrue(state.get().enabled());
      assertEquals(0,state.get().bounds().width());
      assertEquals(0,state.get().bounds().height());
      stack.pop();
      stack.pop();
      assertEquals(original,state.get());
   }

   @Test void touchingGuiEdgesRemainEmptyAtFractionalScale() {
      var state = new AtomicReference<>(new ScissorScopeStack.Snapshot(false,bounds(0,0,0,0)));
      var stack = new ScissorScopeStack(state::get, state::set, b -> b.toFramebuffer(1.5,100,100));
      stack.push(bounds(0,0,1,4), false);
      stack.push(bounds(1,0,1,4), true);
      assertEquals(0,state.get().bounds().width());
      stack.pop();
      stack.pop();
   }

   @Test void failedPushRestoresPreviousStateAndDoesNotPublishFrame() {
      var original = enabled(0,0,20,20);
      var state = new AtomicReference<>(original);
      var fail = new AtomicBoolean(true);
      var stack = new ScissorScopeStack(state::get, s -> {
         state.set(s);
         if (fail.getAndSet(false)) throw new IllegalStateException("injected");
      }, b -> b);
      assertThrows(IllegalStateException.class, () -> stack.push(bounds(0,0,2,2), false));
      assertEquals(original,state.get());
      assertThrows(NoSuchElementException.class,stack::pop);
   }

   @Test void failedRestorationCanBeRetried() {
      var original = enabled(0,0,20,20);
      var state = new AtomicReference<>(original);
      var fail = new AtomicBoolean(false);
      var stack = new ScissorScopeStack(state::get, s -> {
         if (fail.getAndSet(false)) throw new IllegalStateException("injected");
         state.set(s);
      }, b -> b);
      stack.push(bounds(0,0,2,2), false);
      fail.set(true);
      assertThrows(IllegalStateException.class,stack::pop);
      stack.pop();
      assertEquals(original,state.get());
   }
}
