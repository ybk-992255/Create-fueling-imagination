package com.fuelingimagination.menu;

import com.fuelingimagination.block.entity.PoweredArtisanTableBlockEntity;
import com.fuelingimagination.content.artisan.ArtisanAction;
import com.fuelingimagination.content.artisan.ArtisanCraftingHandler;
import com.fuelingimagination.content.artisan.ArtisanModeHandlers;
import com.fuelingimagination.content.artisan.ArtisanProcessStage;
import com.fuelingimagination.content.artisan.ArtisanWorkMode;
import com.fuelingimagination.content.artisan.DamagedBlockRepairHandler;
import com.fuelingimagination.recipe.ArtisanCraftingGrid;
import com.fuelingimagination.recipe.ArtisanRecipeLookup;
import com.fuelingimagination.registry.ModItems;
import com.fuelingimagination.registry.ModMenus;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class PoweredArtisanTableMenu extends AbstractContainerMenu {
   public static final int PREVIOUS_MODE = 0;
   public static final int NEXT_MODE = 1;
   public static final int CRANK = 2;
   public static final int CONFIRM = 3;
   public static final int CANCEL = 4;
   public static final int CLEAN_PART_START = 32;
   public static final int PLAYER_START = 28;
   public static final int HOTBAR_START = 55;
   public static final int PLAYER_END = 64;
   private final BlockPos pos;
   private final Level level;
   private final Player player;
   private final ContainerData data;
   private final SimpleContainer inventory;
   private final SimpleContainer repairParts;
   private final PoweredArtisanTableBlockEntity boundTable;

   public PoweredArtisanTableMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
      this(id, playerInventory, buffer.readBlockPos(), null);
   }

   public PoweredArtisanTableMenu(int id, Inventory playerInventory, PoweredArtisanTableBlockEntity table) {
      this(id, playerInventory, table.getBlockPos(), table);
   }

   private PoweredArtisanTableMenu(int id, Inventory playerInventory, BlockPos pos, final PoweredArtisanTableBlockEntity table) {
      super((MenuType)ModMenus.POWERED_ARTISAN_TABLE.get(), id);
      this.pos = pos;
      this.level = playerInventory.player.level();
      this.player = playerInventory.player;
      this.boundTable = table;
      this.inventory = table == null ? new SimpleContainer(19) : table.getInventory();
      this.repairParts = table == null ? new SimpleContainer(9) : table.getRepairParts();
      this.data = (ContainerData)(table == null ? new SimpleContainerData(23) : new ContainerData() {
         public int get(int index) {
            if (index >= 14 && index <= 22) {
               return table.getExpectedPartId(index - 14);
            } else {
               return switch (index) {
                  case 0 -> table.getWorkMode().ordinal();
                  case 1 -> table.getProcessStage().ordinal();
                  case 2 -> table.getOpenSlots();
                  case 3 -> table.getDirtyParts();
                  default -> table.getCleanProgress(index - 4);
                  case 13 -> table.isCrafting() ? 1 : 0;
               };
            }
         }

         public void set(int index, int value) {
         }

         public int getCount() {
            return 23;
         }
      });
      this.addDataSlots(this.data);

      for (int i = 0; i < 9; i++) {
         this.addMachineSlot(i, 75 + i % 3 * 18, 65 + i / 3 * 18);
      }

      this.addMachineSlot(9, 176, 122);
      this.addMachineSlot(10, 90, 92);

      for (int i = 0; i < 8; i++) {
         this.addMachineSlot(11 + i, ArtisanMenuLayout.RING[i][0], ArtisanMenuLayout.RING[i][1]);
      }

      for (int i = 0; i < 9; i++) {
         final int part = i;
         this.addSlot(
            new Slot(this.repairParts, i, 75 + i % 3 * 18, 65 + i / 3 * 18) {
               public boolean mayPickup(Player player) {
                  return PoweredArtisanTableMenu.this.isRepairDisassembled()
                     && !PoweredArtisanTableMenu.this.isDirtyPart(part)
                     && !PoweredArtisanTableMenu.this.getRepairPart(part).isEmpty();
               }

               public boolean mayPlace(ItemStack stack) {
                  if (!PoweredArtisanTableMenu.this.isRepairDisassembled()
                     || PoweredArtisanTableMenu.this.isDirtyPart(part)
                     || !PoweredArtisanTableMenu.this.getRepairPart(part).isEmpty()) {
                     return false;
                  } else if (PoweredArtisanTableMenu.this.boundTable != null) {
                     return PoweredArtisanTableMenu.this.boundTable.acceptsPart(part, stack);
                  } else {
                     int id = PoweredArtisanTableMenu.this.getExpectedPartId(part);
                     return id >= 0 && stack.getItem() == Item.byId(id);
                  }
               }

               public boolean isActive() {
                  return PoweredArtisanTableMenu.this.isRepairDisassembled();
               }

               public int getMaxStackSize() {
                  return 1;
               }

               public void set(ItemStack stack) {
                  super.set(stack);
                  if (PoweredArtisanTableMenu.this.boundTable != null && !PoweredArtisanTableMenu.this.level.isClientSide && !stack.isEmpty()) {
                     PoweredArtisanTableMenu.this.boundTable.onPartPlaced(part);
                  }
               }

               public void onTake(Player player, ItemStack stack) {
                  super.onTake(player, stack);
                  if (PoweredArtisanTableMenu.this.boundTable != null && !player.level().isClientSide) {
                     PoweredArtisanTableMenu.this.boundTable.onPartTaken(part, stack, player);
                  }
               }
            }
         );
      }

      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 60 + column * 18, 153 + row * 18));
         }
      }

      for (int column = 0; column < 9; column++) {
         this.addSlot(new Slot(playerInventory, column, 60 + column * 18, 211));
      }
   }

   private void addMachineSlot(int index, int x, int y) {
      this.addSlot(
         new Slot(this.inventory, index, x, y) {
            public boolean isActive() {
               if (this.index < 10) {
                  return PoweredArtisanTableMenu.this.getWorkMode() == ArtisanWorkMode.MANUFACTURE;
               } else {
                  return this.index == 10
                     ? PoweredArtisanTableMenu.this.getWorkMode() == ArtisanWorkMode.MODIFY
                        || PoweredArtisanTableMenu.this.getWorkMode() == ArtisanWorkMode.REPAIR && !PoweredArtisanTableMenu.this.isRepairDisassembled()
                     : PoweredArtisanTableMenu.this.getWorkMode() == ArtisanWorkMode.MODIFY && this.index - 11 < PoweredArtisanTableMenu.this.getOpenSlots();
               }
            }

            public boolean mayPlace(ItemStack stack) {
               // Damaged blocks are rejected everywhere except repair mode, which needs them.
               ArtisanWorkMode mode = PoweredArtisanTableMenu.this.getWorkMode();
               if (com.fuelingimagination.content.damaged.DamagedBlocks.isDamaged(stack)
                     && (mode == ArtisanWorkMode.MANUFACTURE || mode == ArtisanWorkMode.MODIFY)) {
                  return false;
               }
               if (this.index < 9 && PoweredArtisanTableMenu.this.isCrafting()) {
                  return false;
               } else if (this.index >= 11 && this.index <= 18
                     && PoweredArtisanTableMenu.this.getWorkMode() == ArtisanWorkMode.MODIFY) {
                  int ring = this.index - 11;
                  java.util.List<com.fuelingimagination.content.upgrade.UpgradeConfig.Slot> slots =
                     com.fuelingimagination.content.upgrade.UpgradeConfig.slotsFor(PoweredArtisanTableMenu.this.getMainItem());
                  // When the client reload produced no layouts, stay optimistic and let the server
                  // (which always loads them) reject a wrong item.
                  return this.isActive() && (!com.fuelingimagination.content.upgrade.UpgradeConfig.isLoaded()
                        || (ring < slots.size() && slots.get(ring).accepts(stack)));
               } else {
                  return this.isActive() && this.index != 9 && (this.index != 10 || !PoweredArtisanTableMenu.this.getProcessStage().locksMainItem())
                     ? this.index != 10 || (PoweredArtisanTableMenu.this.getWorkMode() == ArtisanWorkMode.REPAIR ? DamagedBlockRepairHandler.supports(stack) : PoweredArtisanTableMenu.this.getWorkMode() != ArtisanWorkMode.MODIFY || !com.fuelingimagination.content.upgrade.UpgradeConfig.isLoaded() || com.fuelingimagination.content.upgrade.UpgradeConfig.isModifiable(stack))
                     : false;
               }
            }

            public boolean mayPickup(Player player) {
               return this.index < 9 && PoweredArtisanTableMenu.this.isCrafting()
                  ? false
                  : this.isActive() && (this.index != 10 || !PoweredArtisanTableMenu.this.getProcessStage().locksMainItem());
            }

            public int getMaxStackSize() {
               return this.index >= 10 ? 1 : super.getMaxStackSize();
            }
         }
      );
   }

   public ArtisanWorkMode getWorkMode() {
      return ArtisanWorkMode.fromIndex(this.data.get(0));
   }

   public ArtisanProcessStage getProcessStage() {
      return ArtisanProcessStage.fromIndex(this.data.get(1));
   }

   public int getOpenSlots() {
      return this.data.get(2);
   }

   public int getDirtyParts() {
      return this.data.get(3);
   }

   public int getCleanProgress(int part) {
      return this.data.get(4 + part);
   }

   public boolean isDirtyPart(int part) {
      return part >= 0 && part < 9 && (this.data.get(3) & 1 << part) != 0;
   }

   public boolean isBrokenPart(int part) {
      return part >= 0 && part < 9 && this.getRepairPart(part).is((Item)ModItems.DAMAGED_COMPONENT.get());
   }

   public int getExpectedPartId(int part) {
      return part >= 0 && part < 9 ? this.data.get(14 + part) : -1;
   }

   public int countBrokenParts() {
      int count = 0;

      for (int i = 0; i < 9; i++) {
         if (this.isBrokenPart(i)) {
            count++;
         }
      }

      return count;
   }

   public int countDirtyParts() {
      return Integer.bitCount(this.data.get(3));
   }

   public boolean isAssemblyIncomplete() {
      for (int i = 0; i < 9; i++) {
         if (this.getExpectedPartId(i) >= 0 && this.getRepairPart(i).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   public boolean isRepairDisassembled() {
      return this.getWorkMode() == ArtisanWorkMode.REPAIR && this.getProcessStage() != ArtisanProcessStage.IDLE;
   }

   public ItemStack getRepairPart(int index) {
      return this.repairParts.getItem(index);
   }

   public ItemStack getMainItem() {
      return this.inventory.getItem(10);
   }

   public boolean isCrafting() {
      return this.data.get(13) != 0;
   }

   public ItemStack getCraftingPreview() {
      ArtisanCraftingGrid grid = new ArtisanCraftingGrid(this.inventory);
      return ArtisanRecipeLookup.find(grid, this.level).map(recipe -> recipe.assemble(grid, this.level)).orElse(ItemStack.EMPTY);
   }

   public List<Component> describeMainItem() {
      return this.getMainItem().isEmpty() ? List.of() : ArtisanModeHandlers.get(this.getWorkMode()).describeMainItem(this.getMainItem(), this.level);
   }

   public boolean stillValid(Player player) {
      if (this.level.getBlockEntity(this.pos) instanceof PoweredArtisanTableBlockEntity table
         && (this.level.isClientSide || table == this.boundTable)
         && !table.isRemoved()
         && table.isSpeedRequirementFulfilled()
         && player.distanceToSqr((double)this.pos.getX() + 0.5, (double)this.pos.getY() + 0.5, (double)this.pos.getZ() + 0.5) <= 64.0) {
         return true;
      }

      return false;
   }

   public boolean clickMenuButton(Player player, int button) {
      if (!this.level.isClientSide && this.stillValid(player) && this.level.getBlockEntity(this.pos) instanceof PoweredArtisanTableBlockEntity table) {
         if (button != 0 && button != 1) {
            int detail = -1;
            ArtisanAction action;
            if (button == 2) {
               action = ArtisanAction.CRANK;
            } else if (button == 3) {
               action = ArtisanAction.CONFIRM;
            } else if (button == 4) {
               action = ArtisanAction.CANCEL;
            } else {
               if (button < 32 || button >= 41) {
                  return false;
               }

               action = ArtisanAction.CLEAN;
               detail = button - 32;
            }

            if (!ArtisanModeHandlers.hasHandler(table.getWorkMode())) {
               String key = table.getWorkMode() == ArtisanWorkMode.REPAIR ? "repair_pending" : "modification_pending";
               return ArtisanCraftingHandler.fail(player, key);
            }

            if (!ArtisanModeHandlers.get(table.getWorkMode()).handleAction(table, player, action, detail)) {
               return false;
            }
         } else {
            if (!table.canSwitchWorkMode() || !this.getCarried().isEmpty()) {
               return ArtisanCraftingHandler.fail(player, "clear_before_shift");
            }

            table.setWorkMode(table.getWorkMode().cycle(button == 0 ? -1 : 1));
         }

         this.broadcastChanges();
         this.level.playSound(null, this.pos, button >= 32 ? SoundEvents.WOOL_HIT : SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.35F, 1.0F);
         return true;
      } else {
         return false;
      }
   }

   public void clicked(int slot, int button, ClickType clickType, Player player) {
      if (this.level.isClientSide || this.stillValid(player)) {
         if (slot < 0 || slot >= 28 || ((Slot)this.slots.get(slot)).isActive()) {
            if (slot < 0 || slot >= 9 || !this.isCrafting()) {
               super.clicked(slot, button, clickType, player);
            }
         }
      }
   }

   public boolean canDragTo(Slot slot) {
      if (slot.index < 9 && this.isCrafting()) {
         return false;
      } else if (!slot.isActive()) {
         return false;
      } else {
         return slot.index >= 19 && slot.index < 28 ? true : slot.index < 19 || slot.index >= 28;
      }
   }

   public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
      return slot.index < 9 && this.isCrafting()
         ? false
         : slot.isActive() && (slot.index < 19 || slot.index >= 28) && (slot.index != 10 || !this.getProcessStage().locksMainItem());
   }

   public ItemStack quickMoveStack(Player player, int index) {
      if (index >= 0 && index < this.slots.size()) {
         Slot slot = (Slot)this.slots.get(index);
         if (slot.isActive() && slot.mayPickup(player) && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            ItemStack original = stack.copy();
            boolean moved;
            if (index < 28) {
               moved = this.moveItemStackTo(stack, 28, 64, true);
            } else {
               moved = this.getWorkMode() == ArtisanWorkMode.MANUFACTURE
                  ? !this.isCrafting() && this.moveItemStackTo(stack, 0, 9, false)
                  : this.moveItemStackTo(stack, 10, this.getWorkMode() == ArtisanWorkMode.MODIFY ? 19 : 11, false);
               if (!moved) {
                  moved = index < 55 ? this.moveItemStackTo(stack, 55, 64, false) : this.moveItemStackTo(stack, 28, 55, false);
               }
            }

            if (!moved) {
               return ItemStack.EMPTY;
            } else {
               if (stack.isEmpty()) {
                  slot.setByPlayer(ItemStack.EMPTY);
               } else {
                  slot.setChanged();
               }

               slot.onTake(player, stack);
               return original;
            }
         } else {
            return ItemStack.EMPTY;
         }
      } else {
         return ItemStack.EMPTY;
      }
   }
}
