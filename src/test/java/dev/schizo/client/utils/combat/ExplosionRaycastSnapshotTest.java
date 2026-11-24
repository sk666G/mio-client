package dev.schizo.client.utils.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.junit.jupiter.api.Test;

class ExplosionRaycastSnapshotTest {
   private static final BlockPos BLOCK = BlockPos.ZERO;

   @Test
   void reportsMissOnlyWhenEveryTraversedPositionWasCaptured() {
      Map<Long, ExplosionRaycastSnapshot.BlockGeometry> blocks = new HashMap<>();
      blocks.put(new BlockPos(0, 0, 0).asLong(), emptyBlock());
      blocks.put(new BlockPos(0, 0, 1).asLong(), emptyBlock());
      blocks.put(new BlockPos(0, 0, 2).asLong(), emptyBlock());
      ExplosionRaycastSnapshot snapshot = snapshot(blocks);

      assertEquals(
         ExplosionRaycastSnapshot.Result.MISS,
         snapshot.raycast(
            new Vec3(0.5D, 0.5D, 0.1D),
            new Vec3(0.5D, 0.5D, 2.9D),
            ExplosionRaycastSnapshot.Query.normal()
         )
      );
   }

   @Test
   void resolvesCapturedLocalShapeAtItsWorldBlockPosition() {
      ExplosionRaycastSnapshot snapshot = snapshot(Map.of(
         new BlockPos(0, 0, 0).asLong(), emptyBlock(),
         new BlockPos(0, 0, 1).asLong(),
         new ExplosionRaycastSnapshot.BlockGeometry(1200.0F, Shapes.block().toAabbs()),
         new BlockPos(0, 0, 2).asLong(), emptyBlock()
      ));

      assertEquals(
         ExplosionRaycastSnapshot.Result.HIT,
         snapshot.raycast(
            new Vec3(0.5D, 0.5D, 0.1D),
            new Vec3(0.5D, 0.5D, 2.9D),
            ExplosionRaycastSnapshot.Query.normal()
         )
      );
   }

   @Test
   void reportsIncompleteInsteadOfTreatingUncapturedBlocksAsAir() {
      ExplosionRaycastSnapshot snapshot = snapshot(Map.of(
         BLOCK.asLong(), emptyBlock(),
         new BlockPos(0, 0, 2).asLong(), emptyBlock()
      ));

      assertEquals(
         ExplosionRaycastSnapshot.Result.INCOMPLETE,
         snapshot.raycast(
            new Vec3(0.5D, 0.5D, 0.1D),
            new Vec3(0.5D, 0.5D, 2.9D),
            ExplosionRaycastSnapshot.Query.normal()
         )
      );
   }

   @Test
   void detectsCapturedCollisionGeometry() {
      ExplosionRaycastSnapshot snapshot = snapshot(Map.of(
         BLOCK.asLong(),
         new ExplosionRaycastSnapshot.BlockGeometry(
            1200.0F,
            List.of(new AABB(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D))
         )
      ));

      assertEquals(
         ExplosionRaycastSnapshot.Result.HIT,
         snapshot.raycast(
            new Vec3(0.1D, 0.25D, 0.5D),
            new Vec3(0.9D, 0.25D, 0.5D),
            ExplosionRaycastSnapshot.Query.normal()
         )
      );
      assertEquals(
         ExplosionRaycastSnapshot.Result.MISS,
         snapshot.raycast(
            new Vec3(0.1D, 0.75D, 0.5D),
            new Vec3(0.9D, 0.75D, 0.5D),
            ExplosionRaycastSnapshot.Query.normal()
         )
      );
   }

   @Test
   void preservesInsideShapeHitBehavior() {
      ExplosionRaycastSnapshot snapshot = snapshot(Map.of(
         BLOCK.asLong(),
         new ExplosionRaycastSnapshot.BlockGeometry(1200.0F, Shapes.block().toAabbs())
      ));

      assertEquals(
         ExplosionRaycastSnapshot.Result.HIT,
         snapshot.raycast(
            new Vec3(0.2D, 0.2D, 0.2D),
            new Vec3(0.8D, 0.8D, 0.8D),
            ExplosionRaycastSnapshot.Query.normal()
         )
      );
   }

   @Test
   void ignoredPositionTakesPrecedenceOverSolidAndResistanceRules() {
      ExplosionRaycastSnapshot snapshot = snapshot(Map.of(
         BLOCK.asLong(), emptyBlock()
      ));
      ExplosionRaycastSnapshot.Query query = new ExplosionRaycastSnapshot.Query(
         Set.of(BLOCK.asLong()),
         BLOCK.asLong(),
         true
      );

      assertEquals(
         ExplosionRaycastSnapshot.Result.MISS,
         snapshot.raycast(
            new Vec3(0.1D, 0.5D, 0.5D),
            new Vec3(0.9D, 0.5D, 0.5D),
            query
         )
      );
   }

   @Test
   void solidPositionTakesPrecedenceOverLowResistanceFilter() {
      ExplosionRaycastSnapshot snapshot = snapshot(Map.of(
         BLOCK.asLong(), emptyBlock()
      ));
      ExplosionRaycastSnapshot.Query query = new ExplosionRaycastSnapshot.Query(
         Set.of(),
         BLOCK.asLong(),
         true
      );

      assertEquals(
         ExplosionRaycastSnapshot.Result.HIT,
         snapshot.raycast(
            new Vec3(0.1D, 0.5D, 0.5D),
            new Vec3(0.9D, 0.5D, 0.5D),
            query
         )
      );
   }

   @Test
   void ignoredPositionDoesNotRequireCapturedGeometry() {
      long ignoredPosition = new BlockPos(-1, -1, -1).asLong();
      ExplosionRaycastSnapshot snapshot = snapshot(Map.of(
         new BlockPos(-1, -1, 0).asLong(), emptyBlock(),
         new BlockPos(-1, 0, 0).asLong(), emptyBlock(),
         BlockPos.ZERO.asLong(), emptyBlock()
      ));
      ExplosionRaycastSnapshot.Query query = new ExplosionRaycastSnapshot.Query(
         Set.of(ignoredPosition),
         null,
         false
      );

      assertEquals(
         ExplosionRaycastSnapshot.Result.MISS,
         snapshot.raycast(
            new Vec3(-0.5D, -0.5D, -0.5D),
            new Vec3(0.5D, 0.5D, 0.5D),
            query
         )
      );
   }

   @Test
   void solidPositionDoesNotRequireCapturedGeometry() {
      long solidPosition = new BlockPos(-1, -1, -1).asLong();
      ExplosionRaycastSnapshot snapshot = snapshot(Map.of(
         new BlockPos(-1, -1, 0).asLong(), emptyBlock(),
         new BlockPos(-1, 0, 0).asLong(), emptyBlock(),
         BlockPos.ZERO.asLong(), emptyBlock()
      ));
      ExplosionRaycastSnapshot.Query query = new ExplosionRaycastSnapshot.Query(
         Set.of(),
         solidPosition,
         true
      );

      assertEquals(
         ExplosionRaycastSnapshot.Result.HIT,
         snapshot.raycast(
            new Vec3(-0.5D, -0.5D, -0.5D),
            new Vec3(0.5D, 0.5D, 0.5D),
            query
         )
      );
   }

   @Test
   void lowResistanceRuleIgnoresOnlyCapturedLowResistanceGeometry() {
      ExplosionRaycastSnapshot snapshot = snapshot(Map.of(
         BLOCK.asLong(),
         new ExplosionRaycastSnapshot.BlockGeometry(
            5.0F,
            Shapes.block().toAabbs()
         )
      ));
      ExplosionRaycastSnapshot.Query query = new ExplosionRaycastSnapshot.Query(Set.of(), null, true);

      assertEquals(
         ExplosionRaycastSnapshot.Result.MISS,
         snapshot.raycast(
            new Vec3(0.1D, 0.5D, 0.5D),
            new Vec3(0.9D, 0.5D, 0.5D),
            query
         )
      );
   }

   @Test
   void queryFactoryIgnoresNullEntriesLikeTheExistingRayClassifier() {
      ExplosionRaycastSnapshot.Query query = ExplosionRaycastSnapshot.Query.fromPositions(
         java.util.Arrays.asList(null, BLOCK),
         null,
         false
      );

      assertEquals(Set.of(BLOCK.asLong()), query.ignoredBlockPositions());
   }

   @Test
   void defensiveCopiesMapAndGeometryLists() {
      List<AABB> boxes = new java.util.ArrayList<>(Shapes.block().toAabbs());
      Map<Long, ExplosionRaycastSnapshot.BlockGeometry> blocks = new HashMap<>();
      blocks.put(BLOCK.asLong(), new ExplosionRaycastSnapshot.BlockGeometry(1200.0F, boxes));
      ExplosionRaycastSnapshot snapshot = snapshot(blocks);

      boxes.clear();
      blocks.clear();

      assertEquals(1, snapshot.capturedBlockCount());
      assertEquals(
         ExplosionRaycastSnapshot.Result.HIT,
         snapshot.raycast(
            new Vec3(0.1D, 0.5D, 0.5D),
            new Vec3(0.9D, 0.5D, 0.5D),
            ExplosionRaycastSnapshot.Query.normal()
         )
      );
   }

   @Test
   void matchesVoxelShapeClipForRepresentativeSingleBlockRays() {
      assertMatchesVanillaClip(
         Shapes.box(0.0D, 0.0D, 0.0D, 0.5D, 1.0D, 1.0D),
         new Vec3(0.8D, 0.5D, 0.5D),
         new Vec3(0.2D, 0.5D, 0.5D)
      );
      assertMatchesVanillaClip(
         Shapes.box(0.0D, 0.0D, 0.0D, 0.5D, 1.0D, 1.0D),
         new Vec3(0.8D, 0.5D, 0.5D),
         new Vec3(0.6D, 0.5D, 0.5D)
      );
      assertMatchesVanillaClip(
         Shapes.box(0.0D, 0.0D, 0.0D, 0.5D, 0.5D, 1.0D),
         new Vec3(0.2D, 0.2D, 0.2D),
         new Vec3(0.4D, 0.4D, 0.4D)
      );
      assertMatchesVanillaClip(
         Shapes.or(
            Shapes.box(0.0D, 0.0D, 0.0D, 0.25D, 1.0D, 1.0D),
            Shapes.box(0.75D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D)
         ),
         new Vec3(0.4D, 0.5D, 0.5D),
         new Vec3(0.6D, 0.5D, 0.5D)
      );
   }

   private static void assertMatchesVanillaClip(
      net.minecraft.world.phys.shapes.VoxelShape shape,
      Vec3 start,
      Vec3 end
   ) {
      boolean expectedHit = shape.clip(start, end, BLOCK) != null;
      ExplosionRaycastSnapshot snapshot = snapshot(Map.of(
         BLOCK.asLong(),
         new ExplosionRaycastSnapshot.BlockGeometry(1200.0F, shape.toAabbs())
      ));
      ExplosionRaycastSnapshot.Result actual = snapshot.raycast(
         start,
         end,
         ExplosionRaycastSnapshot.Query.normal()
      );

      assertEquals(
         expectedHit ? ExplosionRaycastSnapshot.Result.HIT : ExplosionRaycastSnapshot.Result.MISS,
         actual
      );
   }

   @Test
   void queryFactoryPacksMutablePositionValuesBeforeTheyCanChange() {
      BlockPos.MutableBlockPos ignored = new BlockPos.MutableBlockPos(2, 3, 4);
      ExplosionRaycastSnapshot.Query query = ExplosionRaycastSnapshot.Query.fromPositions(
         List.of(ignored),
         new BlockPos(5, 6, 7),
         true
      );
      ignored.set(100, 100, 100);

      assertEquals(Set.of(new BlockPos(2, 3, 4).asLong()), query.ignoredBlockPositions());
      assertEquals(new BlockPos(5, 6, 7).asLong(), query.solidBlockPosition());
      assertEquals(true, query.ignoreLowResistance());
   }

   @Test
   void requiresNonNullSnapshotMetadataAndQueryValues() {
      assertThrows(NullPointerException.class,
         () -> new ExplosionRaycastSnapshot(1L, 1L, null, Map.of()));
      assertThrows(NullPointerException.class,
         () -> new ExplosionRaycastSnapshot(1L, 1L, "minecraft:overworld", null));
      assertThrows(NullPointerException.class,
         () -> new ExplosionRaycastSnapshot.Query(null, null, false));
      assertThrows(NullPointerException.class,
         () -> snapshot(Map.of()).raycast(null, Vec3.ZERO, ExplosionRaycastSnapshot.Query.normal()));
   }

   private static ExplosionRaycastSnapshot snapshot(
      Map<Long, ExplosionRaycastSnapshot.BlockGeometry> blocks
   ) {
      return new ExplosionRaycastSnapshot(4L, 12L, "minecraft:overworld", blocks);
   }

   private static ExplosionRaycastSnapshot.BlockGeometry emptyBlock() {
      return new ExplosionRaycastSnapshot.BlockGeometry(0.0F, List.of());
   }
}
