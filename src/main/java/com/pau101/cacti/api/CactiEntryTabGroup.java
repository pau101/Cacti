package com.pau101.cacti.api;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * An entry containing CreativeTabs.
 */
public final class CactiEntryTabGroup extends CactiEntry {
	/**
	 * The list of tabs contained within this tab group
	 */
	private final List<CreativeTabs> tabs;

	CactiEntryTabGroup(String id, CactiEntryCategory owner) {
		super(id, owner);
		tabs = new ArrayList<CreativeTabs>();
	}

	/**
	 * Returns the number of tabs in this tab group.
	 *
	 * @return the number of tabs in this tab group
	 */
	public int size() {
		return tabs.size();
	}

	/**
	 * Returns an immutable copy of the tabs
	 * within this tab group.
	 *
	 * @return an immutable copy of {@link #tabs}
	 */
	public ImmutableList<CreativeTabs> getTabs() {
		return ImmutableList.copyOf(tabs);
	}

	@Override
	public boolean contains(CreativeTabs tab) {
		return tabs.contains(tab);
	}

	/**
	 * Adds the specified tab to this tab group.
	 *
	 * @param tab the CreativeTabs to be added to this tab group
	 * @return a reference to this {@code CactiEntryTabGroup} object
	 * @throws IllegalArgumentException If the {@code tab} is null
	 *         or is already present in this tab group
	 */
	public CactiEntryTabGroup withTab(CreativeTabs tab) {
		Preconditions.checkNotNull(tab, "Tab must be non-null");
		Preconditions.checkArgument(!tabs.contains(tab), "Tab is already present: %s", tab.getTabLabel());
		tabs.add(tab);
		return this;
	}
}
