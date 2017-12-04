import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import com.pau101.cacti.Cacti;
import com.pau101.cacti.api.CactiAPI;

@Mod(modid = "cactitest", name = "Cacti Test", version = "1.0.0", dependencies = "after:" + Cacti.MODID)
public final class CactiTest {
	public static final String ID = "cactitest";

	@Mod.EventHandler
	public void init(FMLPreInitializationEvent event) {
		if (Loader.isModLoaded(Cacti.MODID)) {
			CactiAPI.categories().withCategory(ID)
				.withTabGroup("group1")
					.withTab(new Tab("tab1", Items.ITEM_FRAME))
					.withTab(new Tab("tab2", Items.APPLE))
					.end()
				.withTabGroup("group2")
					.withTab(new Tab("tab3", Items.BEETROOT_SEEDS))
					.withTab(new Tab("tab4", Items.CHORUS_FRUIT_POPPED));

		}
	}

	private static final class Tab extends CreativeTabs {
		private final ItemStack item;

		private Tab(String name, Item item) {
			super(name);
			this.item = new ItemStack(item);
		}

		@Override
		public ItemStack getTabIconItem() {
			return item;
		}
	}
}
