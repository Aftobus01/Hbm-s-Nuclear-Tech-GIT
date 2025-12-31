package com.hbm.inventory.recipes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static com.hbm.inventory.OreDictManager.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.config.GeneralConfig;
import com.hbm.inventory.FluidStack;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.machine.ItemCircuit.EnumCircuitType;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class SolderingRecipes extends SerializableRecipe {

	public static List<SolderingRecipe> recipes = new ArrayList();

	@Override
	public void registerDefaults() {

		boolean lbsm = GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleCrafting;
		boolean no528 = !GeneralConfig.enable528;

		/*
		 * CIRCUITS
		 */

		recipes.add(new SolderingRecipe(new ItemStack(ModItems.circuit, 1, EnumCircuitType.ANALOG.ordinal()), 100, 100,
				new AStack[] {
						new ComparableStack(ModItems.circuit, 3, EnumCircuitType.VACUUM_TUBE),
						new ComparableStack(ModItems.circuit, 2, EnumCircuitType.CAPACITOR)},
				new AStack[] {
						new ComparableStack(ModItems.circuit, 4, EnumCircuitType.PCB)},
				new AStack[] {
						new OreDictStack(PB.wireFine(), 4)}
		));

		recipes.add(new SolderingRecipe(new ItemStack(ModItems.circuit, 1, EnumCircuitType.BASIC.ordinal()), 200, 250,
				new AStack[] {
						new ComparableStack(ModItems.circuit, 4, EnumCircuitType.CHIP)},
				new AStack[] {
						new ComparableStack(ModItems.circuit, 4, EnumCircuitType.PCB)},
				new AStack[] {
						new OreDictStack(PB.wireFine(), 4)}
		));

		recipes.add(new SolderingRecipe(new ItemStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED.ordinal()), 300, 1_000,
				new FluidStack(Fluids.SULFURIC_ACID, 1_000),
				new AStack[] {
						new ComparableStack(ModItems.circuit, lbsm ? 4 : 16, EnumCircuitType.CHIP),
						new ComparableStack(ModItems.circuit, 4, EnumCircuitType.CAPACITOR)},
				new AStack[] {
						new ComparableStack(ModItems.circuit, 8, EnumCircuitType.PCB),
						new OreDictStack(RUBBER.ingot(), 2)},
				new AStack[] {
						new OreDictStack(PB.wireFine(), 8)}
		));

		recipes.add(new SolderingRecipe(new ItemStack(ModItems.circuit, 1, EnumCircuitType.CAPACITOR_BOARD.ordinal()), 200, 300,
				new FluidStack(Fluids.PEROXIDE, 250),
				new AStack[] {
						new ComparableStack(ModItems.circuit, 3, EnumCircuitType.CAPACITOR_TANTALIUM)},
				new AStack[] {
						new ComparableStack(ModItems.circuit, 1, EnumCircuitType.PCB)},
				new AStack[] {
						new OreDictStack(PB.wireFine(), 3)}
		));

		recipes.add(new SolderingRecipe(new ItemStack(ModItems.circuit, 1, EnumCircuitType.BISMOID.ordinal()), 400, 10_000,
				new FluidStack(Fluids.SOLVENT, 1_000),
				new AStack[] {
						new ComparableStack(ModItems.circuit, 4, EnumCircuitType.CHIP_BISMOID),
						new ComparableStack(ModItems.circuit, lbsm ? 4 : 16, EnumCircuitType.CHIP),
						new ComparableStack(ModItems.circuit, lbsm ? 8 : 24, EnumCircuitType.CAPACITOR)},
				new AStack[] {
						new ComparableStack(ModItems.circuit, 12, EnumCircuitType.PCB),
						new OreDictStack(ANY_HARDPLASTIC.ingot(), 2)},
				new AStack[] {
						new OreDictStack(PB.wireFine(), 12)}
		));

		recipes.add(new SolderingRecipe(new ItemStack(ModItems.circuit, 1, EnumCircuitType.QUANTUM.ordinal()), 400, 100_000,
				new FluidStack(Fluids.HELIUM4, 1_000),
				new AStack[] {
						new ComparableStack(ModItems.circuit, 4, EnumCircuitType.CHIP_QUANTUM),
						new ComparableStack(ModItems.circuit, lbsm ? 4 : 16, EnumCircuitType.CHIP_BISMOID),
						new ComparableStack(ModItems.circuit, lbsm ? 1 : 4, EnumCircuitType.ATOMIC_CLOCK)},
				new AStack[] {
						new ComparableStack(ModItems.circuit, 16, EnumCircuitType.PCB),
						new OreDictStack(ANY_HARDPLASTIC.ingot(), 4)},
				new AStack[] {
						new OreDictStack(PB.wireFine(), 16)}
		));

		/*
		 * COMPUTERS
		 */

		if(no528) {
			recipes.add(new SolderingRecipe(new ItemStack(ModItems.circuit, 1, EnumCircuitType.CONTROLLER.ordinal()), 400, 15_000,
					new FluidStack(Fluids.PERFLUOROMETHYL, 1_000),
					new AStack[] {
							new ComparableStack(ModItems.circuit, lbsm ? 8 : 32, EnumCircuitType.CHIP),
							new ComparableStack(ModItems.circuit, lbsm ? 8 : 32, EnumCircuitType.CAPACITOR),
							new ComparableStack(ModItems.circuit, lbsm ? 8 : 16, EnumCircuitType.CAPACITOR_TANTALIUM)},
					new AStack[] {
							new ComparableStack(ModItems.circuit, 1, EnumCircuitType.CONTROLLER_CHASSIS),
							new ComparableStack(ModItems.upgrade_speed_1)},
					new AStack[] {
							new OreDictStack(PB.wireFine(), 16)}
			));
			recipes.add(new SolderingRecipe(new ItemStack(ModItems.circuit, 1, EnumCircuitType.CONTROLLER_ADVANCED.ordinal()), 600, 25_000,
					new FluidStack(Fluids.PERFLUOROMETHYL, 4_000),
					new AStack[] {
							new ComparableStack(ModItems.circuit, lbsm ? 8 : 16, EnumCircuitType.CHIP_BISMOID),
							new ComparableStack(ModItems.circuit, lbsm ? 16 : 48, EnumCircuitType.CAPACITOR_TANTALIUM),
							new ComparableStack(ModItems.circuit, lbsm ? 8 : 32, EnumCircuitType.CAPACITOR_LANTHANIUM)},
					new AStack[] {
							new ComparableStack(ModItems.circuit, 1, EnumCircuitType.CONTROLLER_CHASSIS),
							new ComparableStack(ModItems.upgrade_speed_3)},
					new AStack[] {
							new OreDictStack(PB.wireFine(), 24)}
			));

		recipes.add(new SolderingRecipe(new ItemStack(ModItems.circuit, 1, EnumCircuitType.CONTROLLER_QUANTUM.ordinal()), 600, 250_000,
					new FluidStack(Fluids.PERFLUOROMETHYL_COLD, 6_000),
					new AStack[] {
							new ComparableStack(ModItems.circuit, lbsm ? 8 : 16, EnumCircuitType.CHIP_QUANTUM),
							new ComparableStack(ModItems.circuit, lbsm ? 16 : 48, EnumCircuitType.CHIP_BISMOID),
							new ComparableStack(ModItems.circuit, lbsm ? 1 : 8, EnumCircuitType.ATOMIC_CLOCK)},
					new AStack[] {
							new ComparableStack(ModItems.circuit, 2, EnumCircuitType.CONTROLLER_ADVANCED),
							new ComparableStack(ModItems.upgrade_overdrive_1)},
					new AStack[] {
							new OreDictStack(PB.wireFine(), 32)}
			));
		}

		/*
		 * SPACE
		 * Includes cheaper recipes from TEKTO!
		 */

		recipes.add(new SolderingRecipe(new ItemStack(ModItems.circuit, 1, EnumCircuitType.AERO.ordinal()), 300, 1_000,
				new AStack[] {
						new ComparableStack(ModItems.circuit, 3, EnumCircuitType.CHIP)},
				new AStack[] {
						new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED),
						new OreDictStack(RUBBER.ingot(), 4)},
				new AStack[] {
						new OreDictStack(PB.wireFine(), 4)}
		));

		recipes.add(new SolderingRecipe(new ItemStack(ModItems.hard_drive, 1), 200, 250,
				new AStack[] {
						new ComparableStack(ModItems.circuit, 2, EnumCircuitType.CHIP)},
				new AStack[] {
						new ComparableStack(ModItems.circuit, 16, EnumCircuitType.PCB)},
				new AStack[] {
						new OreDictStack(MINGRADE.wireFine(), 4)}
		));

		recipes.add(new SolderingRecipe(new ItemStack(ModItems.circuit, 1, EnumCircuitType.BISMOID.ordinal()), 400, 10_000,
				new FluidStack(Fluids.POLYTHYLENE, 1_000),
				new AStack[] {
						new ComparableStack(ModItems.circuit, 4, EnumCircuitType.CHIP_BISMOID),
						new ComparableStack(ModItems.circuit, lbsm ? 1 : 4, EnumCircuitType.GASCHIP),
						new ComparableStack(ModItems.circuit, lbsm ? 2 : 8, EnumCircuitType.CAPACITOR)},
				new AStack[] {
						new ComparableStack(ModItems.circuit, 6, EnumCircuitType.PCB)},
				new AStack[] {
						new OreDictStack(PB.wireFine(), 12)}
		));

		recipes.add(new SolderingRecipe(new ItemStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED.ordinal()), 300, 1_000,
				new FluidStack(Fluids.POLYTHYLENE, 250),
				new AStack[] {
						new ComparableStack(ModItems.circuit, lbsm ? 1 : 2, EnumCircuitType.GASCHIP),
						new ComparableStack(ModItems.circuit, 2, EnumCircuitType.CAPACITOR)},
				new AStack[] {
						new ComparableStack(ModItems.circuit, 4, EnumCircuitType.PCB)},
				new AStack[] {
						new OreDictStack(PB.wireFine(), 8)}
		));

		/*
		 * UPGRADES
		 */

		recipes.add(new SolderingRecipe(new ItemStack(ModItems.upgrade_speed_1), 200, 1_000,
				new AStack[] {new ComparableStack(ModItems.circuit, 4, EnumCircuitType.VACUUM_TUBE), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.CAPACITOR)},
				new AStack[] {new ComparableStack(ModItems.upgrade_template), new OreDictStack(MINGRADE.dust(), 4)},
				new AStack[] {}
		));
		recipes.add(new SolderingRecipe(new ItemStack(ModItems.upgrade_effect_1), 200, 1_000,
				new AStack[] {new ComparableStack(ModItems.circuit, 4, EnumCircuitType.VACUUM_TUBE), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.CAPACITOR)},
				new AStack[] {new ComparableStack(ModItems.upgrade_template), new OreDictStack(EMERALD.dust(), 4)},
				new AStack[] {}
		));
		recipes.add(new SolderingRecipe(new ItemStack(ModItems.upgrade_power_1), 200, 1_000,
				new AStack[] {new ComparableStack(ModItems.circuit, 4, EnumCircuitType.VACUUM_TUBE), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.CAPACITOR)},
				new AStack[] {new ComparableStack(ModItems.upgrade_template), new OreDictStack(GOLD.dust(), 4)},
				new AStack[] {}
		));
		recipes.add(new SolderingRecipe(new ItemStack(ModItems.upgrade_fortune_1), 200, 1_000,
				new AStack[] {new ComparableStack(ModItems.circuit, 4, EnumCircuitType.VACUUM_TUBE), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.CAPACITOR)},
				new AStack[] {new ComparableStack(ModItems.upgrade_template), new OreDictStack(NB.dust(), 4)},
				new AStack[] {}
		));
		recipes.add(new SolderingRecipe(new ItemStack(ModItems.upgrade_afterburn_1), 200, 1_000,
				new AStack[] {new ComparableStack(ModItems.circuit, 4, EnumCircuitType.VACUUM_TUBE), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.CAPACITOR)},
				new AStack[] {new ComparableStack(ModItems.upgrade_template), new OreDictStack(W.dust(), 4)},
				new AStack[] {}
		));
		recipes.add(new SolderingRecipe(new ItemStack(ModItems.upgrade_radius), 200, 1_000,
				new AStack[] {new ComparableStack(ModItems.circuit, 4, EnumCircuitType.CHIP), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.CAPACITOR)},
				new AStack[] {new ComparableStack(ModItems.upgrade_template), new OreDictStack("dustGlowstone", 4)},
				new AStack[] {}
		));
		recipes.add(new SolderingRecipe(new ItemStack(ModItems.upgrade_health), 200, 1_000,
				new AStack[] {new ComparableStack(ModItems.circuit, 4, EnumCircuitType.CHIP), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.CAPACITOR)},
				new AStack[] {new ComparableStack(ModItems.upgrade_template), new OreDictStack(LI.dust(), 4)},
				new AStack[] {}
		));



		if(no528) {
			addFirstUpgrade(ModItems.upgrade_speed_1, ModItems.upgrade_speed_2);
			addSecondUpgrade(ModItems.upgrade_speed_2, ModItems.upgrade_speed_3);
			addFirstUpgrade(ModItems.upgrade_effect_1, ModItems.upgrade_effect_2);
			addSecondUpgrade(ModItems.upgrade_effect_2, ModItems.upgrade_effect_3);
			addFirstUpgrade(ModItems.upgrade_power_1, ModItems.upgrade_power_2);
			addSecondUpgrade(ModItems.upgrade_power_2, ModItems.upgrade_power_3);
			addFirstUpgrade(ModItems.upgrade_fortune_1, ModItems.upgrade_fortune_2);
			addSecondUpgrade(ModItems.upgrade_fortune_2, ModItems.upgrade_fortune_3);
			addFirstUpgrade(ModItems.upgrade_afterburn_1, ModItems.upgrade_afterburn_2);
			addSecondUpgrade(ModItems.upgrade_afterburn_2, ModItems.upgrade_afterburn_3);
		}



		// NTNH Recipes (brainpain)
		if(Loader.isModLoaded("openfm") && Loader.isModLoaded("opencomputers")) {
			recipes.add(new SolderingRecipe(
				new ItemStack(GameRegistry.findItem("openfm", "MemoryCard"), 1, 0),
				80,
				100,
				new AStack[] {
					new OreDictStack("wireFineCopper", 8),
					new ComparableStack(GameRegistry.findItem("opencomputers", "oc:floppy"), 1)
				},
				new AStack[] {},
				new AStack[] {}
			));
		}

		if(Loader.isModLoaded("appliedenergistics2") && Loader.isModLoaded("hbm")) {
			Item ae2ItemMultiMaterial = GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial");
			Item hbmItemPlatePolymer = GameRegistry.findItem("hbm", "item.plate_polymer");
			Item hbmItemIngotDesh = GameRegistry.findItem("hbm", "item.ingot_desh");
			Item hbmItemIngotTcalloy = GameRegistry.findItem("hbm", "item.ingot_tcalloy");
			Item hbmItemNuggetTechnetium = GameRegistry.findItem("hbm", "item.nugget_technetium");

			recipes.add(new SolderingRecipe(
				new ItemStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 0),
				100,
				150,
				new AStack[] {
					new ComparableStack(ae2ItemMultiMaterial, 8, 10),
					new ComparableStack(hbmItemPlatePolymer, 16),
					new ComparableStack(hbmItemIngotDesh, 1)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 32)
				}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 1),
				100,
				150,
				new AStack[] {
					new ComparableStack(ae2ItemMultiMaterial, 8, 10),
					new ComparableStack(hbmItemPlatePolymer, 16),
					new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 3, 0)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 32)
				}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 2),
				100,
				150,
				new AStack[] {
					new ComparableStack(ae2ItemMultiMaterial, 8, 10),
					new ComparableStack(hbmItemPlatePolymer, 16),
					new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 3, 1)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 32)
				}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 3),
				100,
				150,
				new AStack[] {
					new ComparableStack(ae2ItemMultiMaterial, 8, 10),
					new ComparableStack(hbmItemPlatePolymer, 16),
					new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 3, 2)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 32)
				}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 4),
				100,
				150,
				new AStack[] {
					new ComparableStack(ae2ItemMultiMaterial, 8, 10),
					new ComparableStack(hbmItemPlatePolymer, 16),
					new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 3, 3)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 32)
				}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 5),
				100,
				150,
				new AStack[] {
					new ComparableStack(ae2ItemMultiMaterial, 8, 10),
					new ComparableStack(hbmItemPlatePolymer, 16),
					new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 3, 4)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 32)
				}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 6),
				100,
				150,
				new AStack[] {
					new ComparableStack(ae2ItemMultiMaterial, 8, 10),
					new ComparableStack(hbmItemPlatePolymer, 16),
					new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 3, 5)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 32)
				}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 7),
				100,
				150,
				new AStack[] {
					new ComparableStack(ae2ItemMultiMaterial, 8, 10),
					new ComparableStack(hbmItemPlatePolymer, 16),
					new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 3, 6)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 32)
				}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ae2ItemMultiMaterial, 1, 35),
				100,
				150,
				new AStack[] {
					new ComparableStack(ae2ItemMultiMaterial, 8, 10),
					new ComparableStack(hbmItemPlatePolymer, 16),
					new ComparableStack(hbmItemIngotTcalloy, 1)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 32)
				}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ae2ItemMultiMaterial, 1, 36),
				100,
				150,
				new AStack[] {
					new ComparableStack(ae2ItemMultiMaterial, 8, 10),
					new ComparableStack(hbmItemPlatePolymer, 16),
					new ComparableStack(ae2ItemMultiMaterial, 3, 35)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 32)
				}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ae2ItemMultiMaterial, 1, 37),
				100,
				150,
				new AStack[] {
					new ComparableStack(ae2ItemMultiMaterial, 8, 10),
					new ComparableStack(hbmItemPlatePolymer, 16),
					new ComparableStack(ae2ItemMultiMaterial, 3, 36)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 32)
				}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ae2ItemMultiMaterial, 1, 38),
				100,
				150,
				new AStack[] {
					new ComparableStack(ae2ItemMultiMaterial, 8, 10),
					new ComparableStack(hbmItemPlatePolymer, 16),
					new ComparableStack(ae2ItemMultiMaterial, 3, 37)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 32)
				}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ae2ItemMultiMaterial, 1, 57),
				100,
				150,
				new AStack[] {
					new ComparableStack(ae2ItemMultiMaterial, 8, 10),
					new ComparableStack(hbmItemPlatePolymer, 16),
					new ComparableStack(ae2ItemMultiMaterial, 3, 38)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 32)
				}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ae2ItemMultiMaterial, 1, 58),
				100,
				150,
				new AStack[] {
					new ComparableStack(ae2ItemMultiMaterial, 8, 10),
					new ComparableStack(hbmItemPlatePolymer, 16),
					new ComparableStack(ae2ItemMultiMaterial, 3, 57)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 32)
				}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ae2ItemMultiMaterial, 1, 59),
				100,
				150,
				new AStack[] {
					new ComparableStack(ae2ItemMultiMaterial, 8, 10),
					new ComparableStack(hbmItemPlatePolymer, 16),
					new ComparableStack(ae2ItemMultiMaterial, 3, 58)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 32)
				}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ae2ItemMultiMaterial, 1, 60),
				100,
				150,
				new AStack[] {
					new ComparableStack(ae2ItemMultiMaterial, 8, 10),
					new ComparableStack(hbmItemPlatePolymer, 16),
					new ComparableStack(ae2ItemMultiMaterial, 3, 59)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 32)
				}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ae2ItemMultiMaterial, 1, 32),
				100,
				150,
				new AStack[] {
					new ComparableStack(ae2ItemMultiMaterial, 8, 9),
					new ComparableStack(hbmItemPlatePolymer, 16),
					new ComparableStack(hbmItemNuggetTechnetium, 2)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 32)
				}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ae2ItemMultiMaterial, 1, 33),
				100,
				150,
				new AStack[] {
					new ComparableStack(ae2ItemMultiMaterial, 8, 9),
					new ComparableStack(hbmItemPlatePolymer, 16),
					new ComparableStack(ae2ItemMultiMaterial, 3, 32)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 32)
				}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ae2ItemMultiMaterial, 1, 34),
				100,
				150,
				new AStack[] {
					new ComparableStack(ae2ItemMultiMaterial, 8, 9),
					new ComparableStack(hbmItemPlatePolymer, 16),
					new ComparableStack(ae2ItemMultiMaterial, 3, 33)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 32)
				}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 24),
				90,
				120,
				new AStack[] {
					new ComparableStack(ae2ItemMultiMaterial, 1, 20),
					new ComparableStack(GameRegistry.findItem("minecraft", "redstone"), 1),
					new ComparableStack(ae2ItemMultiMaterial, 1, 17)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 8)
				}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 22),
				90,
				120,
				new AStack[] {
					new ComparableStack(ae2ItemMultiMaterial, 1, 20),
					new ComparableStack(GameRegistry.findItem("minecraft", "redstone"), 1),
					new ComparableStack(ae2ItemMultiMaterial, 1, 18)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 8)
				}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 23),
				90,
				120,
				new AStack[] {
					new ComparableStack(ae2ItemMultiMaterial, 1, 20),
					new ComparableStack(GameRegistry.findItem("minecraft", "redstone"), 1),
					new ComparableStack(ae2ItemMultiMaterial, 1, 16)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 8)
				}
			));
		}

		if(Loader.isModLoaded("ae2fc") && Loader.isModLoaded("hbm")) {
			Item ae2fcFluidPart = GameRegistry.findItem("ae2fc", "fluid_part");
			Item hbmItemCircuit = GameRegistry.findItem("hbm", "item.circuit");

			recipes.add(new SolderingRecipe(
				new ItemStack(GameRegistry.findItem("ae2fc", "fluid_storage.quantum"), 1),
				300,
				1500,
				new AStack[] {
					new ComparableStack(ae2fcFluidPart, 1, 7),
					new ComparableStack(hbmItemCircuit, 1, 16),
					new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 60)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 64)
				}
			));
		}

		if(Loader.isModLoaded("appliedenergistics2") && Loader.isModLoaded("hbm") && Loader.isModLoaded("ae2fc")) {
			Item ae2ItemExtremeStorageCell = GameRegistry.findItem("appliedenergistics2", "item.ItemExtremeStorageCell.Container");
			Item ae2ItemExtremeStorageCellQuantum = GameRegistry.findItem("appliedenergistics2", "item.ItemExtremeStorageCell.Quantum");
			Item hbmItemCircuit = GameRegistry.findItem("hbm", "item.circuit");

			recipes.add(new SolderingRecipe(
				new ItemStack(ae2ItemExtremeStorageCellQuantum, 1),
				300,
				1500,
				new AStack[] {
					new ComparableStack(ae2ItemExtremeStorageCell, 1),
					new ComparableStack(hbmItemCircuit, 1, 16),
					new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 60)
				},
				new AStack[] {},
				new AStack[] {
					new OreDictStack("wireFineLead", 64)
				}
			));
		}

		if(Loader.isModLoaded("opencomputers") && Loader.isModLoaded("hbm")) {
			Item ocItem = GameRegistry.findItem("opencomputers", "OpenComputers:item");
			Item ocCable = GameRegistry.findItem("opencomputers", "OpenComputers:cable");
			Item ocKeyboard = GameRegistry.findItem("opencomputers", "OpenComputers:keyboard");
			Item hbmItemIngotPolymer = GameRegistry.findItem("hbm", "item.ingot_polymer");
			Item hbmItemCircuit = GameRegistry.findItem("hbm", "item.circuit");
			Item hbmItemNuggetSilicon = GameRegistry.findItem("hbm", "item.nugget_silicon");
			Item hbmItemDrone = GameRegistry.findItem("hbm", "item.drone");

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 23),
				90,
				120,
				new AStack[]{
					new ComparableStack(GameRegistry.findItem("minecraft", "redstone"), 1),
					new ComparableStack(hbmItemNuggetSilicon, 1)
				},
				new AStack[]{
					new ComparableStack(hbmItemIngotPolymer, 1)
				},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 8, 24),
				120,
				200,
				new AStack[]{
					new ComparableStack(GameRegistry.findItem("minecraft", "redstone"), 4),
					new ComparableStack(hbmItemCircuit, 1, 4),
					new ComparableStack(ocItem, 4, 23)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 4, 25),
				120,
				200,
				new AStack[]{
					new ComparableStack(GameRegistry.findItem("minecraft", "redstone"), 4),
					new ComparableStack(ocItem, 8, 24),
					new ComparableStack(ocItem, 4, 23)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 2, 26),
				120,
				200,
				new AStack[]{
					new ComparableStack(GameRegistry.findItem("minecraft", "redstone"), 4),
					new ComparableStack(ocItem, 4, 25),
					new ComparableStack(ocItem, 4, 23)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 27),
				150,
				300,
				new AStack[]{
					new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 23),
					new ComparableStack(ocItem, 1, 24),
					new ComparableStack(ocItem, 3, 23)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 28),
				150,
				300,
				new AStack[]{
					new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 22),
					new ComparableStack(ocItem, 1, 24),
					new ComparableStack(ocItem, 3, 23)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 29),
				180,
				400,
				new AStack[]{
					new ComparableStack(ocItem, 1, 27),
					new ComparableStack(ocItem, 1, 28),
					new ComparableStack(ocItem, 1, 24)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 42),
				180,
				400,
				new AStack[]{
					new ComparableStack(ocItem, 1, 27),
					new ComparableStack(ocItem, 1, 28),
					new ComparableStack(ocItem, 1, 25)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 43),
				180,
				400,
				new AStack[]{
					new ComparableStack(ocItem, 1, 27),
					new ComparableStack(ocItem, 1, 28),
					new ComparableStack(ocItem, 1, 26)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 1),
				100,
				150,
				new AStack[]{
					new ComparableStack(GameRegistry.findItem("minecraft", "redstone"), 4),
					new ComparableStack(ocItem, 2, 24)
				},
				new AStack[]{
					new ComparableStack(hbmItemCircuit, 1, 3)
				},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 2),
				100,
				150,
				new AStack[]{
					new ComparableStack(GameRegistry.findItem("minecraft", "redstone"), 4),
					new ComparableStack(ocItem, 2, 25)
				},
				new AStack[]{
					new ComparableStack(hbmItemCircuit, 1, 3)
				},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 38),
				100,
				150,
				new AStack[]{
					new ComparableStack(GameRegistry.findItem("minecraft", "redstone"), 4),
					new ComparableStack(ocItem, 2, 26)
				},
				new AStack[]{
					new ComparableStack(hbmItemCircuit, 1, 3)
				},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 33),
				90,
				120,
				new AStack[]{
					new ComparableStack(GameRegistry.findItem("minecraft", "redstone"), 4)
				},
				new AStack[]{
					new ComparableStack(hbmItemCircuit, 1, 3),
					new ComparableStack(hbmItemIngotPolymer, 1)
				},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 8),
				180,
				400,
				new AStack[]{
					new ComparableStack(ocItem, 1, 27),
					new ComparableStack(ocItem, 1, 33),
					new ComparableStack(ocItem, 1, 1)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 9),
				180,
				400,
				new AStack[]{
					new ComparableStack(ocItem, 1, 27),
					new ComparableStack(ocItem, 1, 33),
					new ComparableStack(ocItem, 1, 2)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 10),
				180,
				400,
				new AStack[]{
					new ComparableStack(ocItem, 1, 27),
					new ComparableStack(ocItem, 1, 33),
					new ComparableStack(ocItem, 1, 38)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 70),
				120,
				200,
				new AStack[]{
					new ComparableStack(ocItem, 1, 28),
					new ComparableStack(ocItem, 1, 24)
				},
				new AStack[]{
					new ComparableStack(hbmItemCircuit, 1, 3)
				},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 71),
				120,
				200,
				new AStack[]{
					new ComparableStack(ocItem, 1, 28),
					new ComparableStack(ocItem, 1, 25)
				},
				new AStack[]{
					new ComparableStack(hbmItemCircuit, 1, 3)
				},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 72),
				120,
				200,
				new AStack[]{
					new ComparableStack(ocItem, 1, 28),
					new ComparableStack(ocItem, 1, 26)
				},
				new AStack[]{
					new ComparableStack(hbmItemCircuit, 1, 3)
				},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 101),
				200,
				500,
				new AStack[]{
					new ComparableStack(ocItem, 1, 42),
					new ComparableStack(ocItem, 1, 71),
					new ComparableStack(ocItem, 1, 9)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 102),
				200,
				500,
				new AStack[]{
					new ComparableStack(ocItem, 1, 43),
					new ComparableStack(ocItem, 1, 72),
					new ComparableStack(ocItem, 1, 10)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 104),
				200,
				500,
				new AStack[]{
					new ComparableStack(ocItem, 1, 33),
					new ComparableStack(ocItem, 1, 29),
					new ComparableStack(ocItem, 1, 24)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 105),
				200,
				500,
				new AStack[]{
					new ComparableStack(ocItem, 1, 33),
					new ComparableStack(ocItem, 1, 42),
					new ComparableStack(ocItem, 1, 25)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 106),
				200,
				500,
				new AStack[]{
					new ComparableStack(ocItem, 1, 33),
					new ComparableStack(ocItem, 1, 43),
					new ComparableStack(ocItem, 1, 26)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 5),
				100,
				150,
				new AStack[]{
					new ComparableStack(ocItem, 2, 24),
					new ComparableStack(ocItem, 3, 19)
				},
				new AStack[]{
					new ComparableStack(hbmItemCircuit, 1, 3)
				},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 6),
				100,
				150,
				new AStack[]{
					new ComparableStack(ocItem, 2, 25),
					new ComparableStack(ocItem, 3, 19)
				},
				new AStack[]{
					new ComparableStack(hbmItemCircuit, 1, 3)
				},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 7),
				100,
				150,
				new AStack[]{
					new ComparableStack(ocItem, 2, 26),
					new ComparableStack(ocItem, 3, 19)
				},
				new AStack[]{
					new ComparableStack(hbmItemCircuit, 1, 3)
				},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 11),
				120,
				200,
				new AStack[]{
					new ComparableStack(ocItem, 1, 33),
					new ComparableStack(ocCable, 1),
					new ComparableStack(ocItem, 1, 24)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 113),
				150,
				300,
				new AStack[]{
					new ComparableStack(ocItem, 1, 33),
					new ComparableStack(ocItem, 1, 11),
					new ComparableStack(ocItem, 1, 24)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 13),
				150,
				300,
				new AStack[]{
					new ComparableStack(ocItem, 1, 33),
					new ComparableStack(ocItem, 1, 113),
					new ComparableStack(ocItem, 1, 25)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 51),
				180,
				400,
				new AStack[]{
					new ComparableStack(ocItem, 2, 26),
					new ComparableStack(GameRegistry.findItem("minecraft", "ender_pearl"), 2),
					new ComparableStack(ocItem, 2, 11)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 44),
				150,
				300,
				new AStack[]{
					new ComparableStack(ocItem, 1, 33),
					new ComparableStack(GameRegistry.findItem("minecraft", "ender_pearl"), 1),
					new ComparableStack(ocItem, 1, 25)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 66),
				120,
				200,
				new AStack[]{
					new ComparableStack(ocItem, 1, 33),
					new ComparableStack(GameRegistry.findItem("minecraft", "redstone_block"), 1),
					new ComparableStack(ocItem, 1, 24)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 12),
				120,
				200,
				new AStack[]{
					new ComparableStack(ocItem, 1, 33),
					new ComparableStack(GameRegistry.findItem("minecraft", "redstone_block"), 1),
					new ComparableStack(ocItem, 1, 25)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 45),
				180,
				400,
				new AStack[]{
					new ComparableStack(ocItem, 1, 70),
					new ComparableStack(ocItem, 1, 1),
					new ComparableStack(ocItem, 1, 24)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 46),
				180,
				400,
				new AStack[]{
					new ComparableStack(ocItem, 1, 71),
					new ComparableStack(ocItem, 1, 2),
					new ComparableStack(ocItem, 1, 25)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 40),
				180,
				400,
				new AStack[]{
					new ComparableStack(ocItem, 1, 72),
					new ComparableStack(ocItem, 1, 38),
					new ComparableStack(ocItem, 1, 26)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 74),
				120,
				200,
				new AStack[]{
					new ComparableStack(ocItem, 1, 70),
					new ComparableStack(ocItem, 1, 24)
				},
				new AStack[]{
					new ComparableStack(hbmItemCircuit, 1, 3)
				},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 92),
				120,
				200,
				new AStack[]{
					new ComparableStack(ocItem, 1, 71),
					new ComparableStack(ocItem, 1, 25)
				},
				new AStack[]{
					new ComparableStack(hbmItemCircuit, 1, 3)
				},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 82),
				120,
				200,
				new AStack[]{
					new ComparableStack(GameRegistry.findItem("minecraft", "redstone_block"), 1),
					new ComparableStack(ocItem, 1, 24)
				},
				new AStack[]{
					new ComparableStack(hbmItemCircuit, 1, 3)
				},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 86),
				120,
				200,
				new AStack[]{
					new ComparableStack(GameRegistry.findItem("minecraft", "redstone_block"), 1),
					new ComparableStack(ocItem, 1, 25)
				},
				new AStack[]{
					new ComparableStack(hbmItemCircuit, 1, 3)
				},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 83),
				200,
				500,
				new AStack[]{
					new ComparableStack(ocItem, 1, 70),
					new ComparableStack(hbmItemDrone, 1),
					new ComparableStack(ocItem, 1, 82)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 87),
				200,
				500,
				new AStack[]{
					new ComparableStack(ocItem, 1, 71),
					new ComparableStack(hbmItemDrone, 1),
					new ComparableStack(ocItem, 1, 86)
				},
				new AStack[]{},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 108),
				150,
				300,
				new AStack[]{
					new ComparableStack(ocItem, 1, 25),
					new ComparableStack(ocItem, 3, 13)
				},
				new AStack[]{
					new ComparableStack(hbmItemCircuit, 1, 3)
				},
				new AStack[]{}
			));

			recipes.add(new SolderingRecipe(
				new ItemStack(ocItem, 1, 41),
				150,
				300,
				new AStack[]{
					new ComparableStack(ocItem, 1, 25),
					new ComparableStack(ocKeyboard, 1),
					new ComparableStack(ocItem, 1, 13)
				},
				new AStack[]{},
				new AStack[]{}
			));
		}
	}

	public static void addFirstUpgrade(Item lower, Item higher) {
		boolean lbsm = GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleCrafting;
		recipes.add(new SolderingRecipe(new ItemStack(higher), 300, 10_000,
				new AStack[] {new ComparableStack(ModItems.circuit, lbsm ? 4 : 8, EnumCircuitType.CHIP), new ComparableStack(ModItems.circuit, lbsm ? 2 : 4, EnumCircuitType.CAPACITOR)},
				new AStack[] {new ComparableStack(lower), new OreDictStack(ANY_PLASTIC.ingot(), 4)},
				new AStack[] {}
		));
	}

	public static void addSecondUpgrade(Item lower, Item higher) {
		boolean lbsm = GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleCrafting;
		recipes.add(new SolderingRecipe(new ItemStack(higher), 400, 25_000,
				new FluidStack(Fluids.SOLVENT, 500),
				new AStack[] {new ComparableStack(ModItems.circuit, lbsm ? 6 : 16, EnumCircuitType.CHIP), new ComparableStack(ModItems.circuit, lbsm ? 4 : 16, EnumCircuitType.CAPACITOR)},
				new AStack[] {new ComparableStack(lower), new OreDictStack(RUBBER.ingot(), 4)},
				new AStack[] {}
		));
	}

	public static SolderingRecipe getRecipe(ItemStack[] inputs) {

		for(SolderingRecipe recipe : recipes) {
			if(matchesIngredients(new ItemStack[] {inputs[0], inputs[1], inputs[2]}, recipe.toppings) &&
					matchesIngredients(new ItemStack[] {inputs[3], inputs[4]}, recipe.pcb) &&
					matchesIngredients(new ItemStack[] {inputs[5]}, recipe.solder)) return recipe;
		}

		return null;
	}

	public static HashMap getRecipes() {

		HashMap<Object, Object> recipes = new HashMap<Object, Object>();

		for(SolderingRecipe recipe : SolderingRecipes.recipes) {

			List ingredients = new ArrayList();
			for(AStack stack : recipe.toppings) ingredients.add(stack);
			for(AStack stack : recipe.pcb) ingredients.add(stack);
			for(AStack stack : recipe.solder) ingredients.add(stack);
			if(recipe.fluid != null) ingredients.add(ItemFluidIcon.make(recipe.fluid));

			recipes.put(ingredients.toArray(), recipe.output);
		}

		return recipes;
	}

	@Override
	public String getFileName() {
		return "hbmSoldering.json";
	}

	@Override
	public Object getRecipeObject() {
		return recipes;
	}

	@Override
	public void deleteRecipes() {
		recipes.clear();
		toppings.clear();
		pcb.clear();
		solder.clear();
	}

	@Override
	public void readRecipe(JsonElement recipe) {
		JsonObject obj = (JsonObject) recipe;

		AStack[] toppings = this.readAStackArray(obj.get("toppings").getAsJsonArray());
		AStack[] pcb = this.readAStackArray(obj.get("pcb").getAsJsonArray());
		AStack[] solder = this.readAStackArray(obj.get("solder").getAsJsonArray());
		FluidStack fluid = obj.has("fluid") ? this.readFluidStack(obj.get("fluid").getAsJsonArray()) : null;
		ItemStack output = this.readItemStack(obj.get("output").getAsJsonArray());
		int duration = obj.get("duration").getAsInt();
		long consumption = obj.get("consumption").getAsLong();

		recipes.add(new SolderingRecipe(output, duration, consumption, fluid, toppings, pcb, solder));
	}

	@Override
	public void writeRecipe(Object obj, JsonWriter writer) throws IOException {
		SolderingRecipe recipe = (SolderingRecipe) obj;

		writer.name("toppings").beginArray();
		for(AStack aStack : recipe.toppings) this.writeAStack(aStack, writer);
		writer.endArray();

		writer.name("pcb").beginArray();
		for(AStack aStack : recipe.pcb) this.writeAStack(aStack, writer);
		writer.endArray();

		writer.name("solder").beginArray();
		for(AStack aStack : recipe.solder) this.writeAStack(aStack, writer);
		writer.endArray();

		if(recipe.fluid != null) {
			writer.name("fluid");
			this.writeFluidStack(recipe.fluid, writer);
		}

		writer.name("output");
		this.writeItemStack(recipe.output, writer);

		writer.name("duration").value(recipe.duration);
		writer.name("consumption").value(recipe.consumption);
	}

	public static HashSet<AStack> toppings = new HashSet();
	public static HashSet<AStack> pcb = new HashSet();
	public static HashSet<AStack> solder = new HashSet();

	public static class SolderingRecipe {

		public AStack[] toppings;
		public AStack[] pcb;
		public AStack[] solder;
		public FluidStack fluid;
		public ItemStack output;
		public int duration;
		public long consumption;

		public SolderingRecipe(ItemStack output, int duration, long consumption, FluidStack fluid, AStack[] toppings, AStack[] pcb, AStack[] solder) {
			this.toppings = toppings;
			this.pcb = pcb;
			this.solder = solder;
			this.fluid = fluid;
			this.output = output;
			this.duration = duration;
			this.consumption = consumption;
			for(AStack t : toppings) SolderingRecipes.toppings.add(t);
			for(AStack t : pcb) SolderingRecipes.pcb.add(t);
			for(AStack t : solder) SolderingRecipes.solder.add(t);
		}

		public SolderingRecipe(ItemStack output, int duration, long consumption, AStack[] toppings, AStack[] pcb, AStack[] solder) {
			this(output, duration, consumption, null, toppings, pcb, solder);
		}
	}
}
