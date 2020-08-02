/*
 * Copyright (c) 2018-2020 C4
 *
 * This file is part of Beacons For All, a mod made for Minecraft.
 *
 * Beacons For All is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Beacons For All is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Beacons For All.  If not, see <https://www.gnu.org/licenses/>.
 */

package top.theillusivec4.beaconsforall.common;

import com.google.common.base.Predicate;
import java.util.List;
import net.minecraft.entity.IEquipable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.tileentity.BeaconTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.beaconsforall.util.ReflectionAccessor;

public class BeaconsForAllEventHandler {

  private static final Predicate<LivingEntity> VALID_CREATURE = living ->
      !(living instanceof PlayerEntity) && isValidCreature(living);

  private static boolean isValidCreature(LivingEntity livingEntity) {
    boolean validType = false;

    switch (BeaconsForAllConfig.creatureType) {
      case TAMED:
        validType = isTamed(livingEntity);
        break;
      case PASSIVE:
        validType = livingEntity instanceof AnimalEntity && !(livingEntity instanceof IMob);
        break;
      case ALL:
        validType = true;
        break;
    }
    boolean validConfig = BeaconsForAllConfig.additionalCreatures.contains(livingEntity.getType());
    return validType || validConfig;
  }

  private static boolean isTamed(LivingEntity livingEntity) {
    boolean flag =
        livingEntity instanceof TameableEntity && ((TameableEntity) livingEntity).isTamed();

    if (!flag) {
      flag = livingEntity instanceof AbstractHorseEntity && ((AbstractHorseEntity) livingEntity).isTame();
    }

    if (!flag) {
      flag = livingEntity instanceof IEquipable && ((IEquipable) livingEntity).isHorseSaddled();
    }
    return flag;
  }

  private static void addBeaconEffectsToCreatures(BeaconTileEntity beacon, World world) {
    int levels = beacon.getLevels();
    Effect primaryEffect = ReflectionAccessor.getPrimaryEffect(beacon);

    if (ReflectionAccessor.getBeamSegments(beacon).isEmpty() || levels <= 0
        || primaryEffect == null) {
      return;
    }

    Effect secondaryEffect = ReflectionAccessor.getSecondaryEffect(beacon);
    BlockPos pos = beacon.getPos();
    double d0 = levels * 10 + 10;
    int i = 0;

    if (levels >= 4 && primaryEffect == secondaryEffect) {
      i = 1;
    }

    int j = (9 + levels * 2) * 20;
    AxisAlignedBB axisalignedbb = (new AxisAlignedBB(pos)).grow(d0)
        .expand(0.0D, world.getHeight(), 0.0D);
    List<LivingEntity> list = world
        .getEntitiesWithinAABB(LivingEntity.class, axisalignedbb, VALID_CREATURE);

    for (LivingEntity entity : list) {
      entity.addPotionEffect(new EffectInstance(primaryEffect, j, i, true, true));
    }

    if (levels >= 4 && primaryEffect != secondaryEffect && secondaryEffect != null) {

      for (LivingEntity entity : list) {
        entity.addPotionEffect(new EffectInstance(secondaryEffect, j, 0, true, true));
      }
    }
  }

  @SubscribeEvent
  public void onWorldTick(TickEvent.WorldTickEvent evt) {
    World world = evt.world;

    if (world.isRemote || evt.phase != TickEvent.Phase.END || world.getGameTime() % 80L != 0L) {
      return;
    }

    for (TileEntity tileentity : world.tickableTileEntities) {

      if (tileentity instanceof BeaconTileEntity && !tileentity.isRemoved() && tileentity
          .hasWorld()) {
        BlockPos blockpos = tileentity.getPos();

        if (world.isAreaLoaded(blockpos, 0) && world.getWorldBorder().contains(blockpos)) {
          addBeaconEffectsToCreatures((BeaconTileEntity) tileentity, world);
        }
      }
    }
  }
}
