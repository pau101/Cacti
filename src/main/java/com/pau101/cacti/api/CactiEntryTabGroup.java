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
	 * The list of tabs by id contained within this tab group
	 */
	private final List<String> tabs;

	CactiEntryTabGroup(String id, CactiEntryCategory owner) {
		super(id, owner);
		tabs = new ArrayList<String>();
	}

	/**
	 * Returns an immutable copy of the tabs
	 * within this tab group.
	 *
	 * @return an immutable copy of {@link #tabs}
	 */
	public ImmutableList<String> getTabs() {
		return ImmutableList.copyOf(tabs);
	}

	@Override
	public boolean contains(CreativeTabs tab) {
		if (tab == null) {
			return false;
		}
		return tabs.contains(tab.getTabLabel());
	}

	/**
	 * Adds the specified tab unique identifier to this tab group.
	 *
	 * @param tabId the id of the CreativeTabs to be added to this tab group
	 * @return a reference to this {@code CactiEntryTabGroup} object
	 * @throws IllegalArgumentException If the {@code tabId} is already
	 *         present in this tab group
	 */
	public CactiEntryTabGroup withTab(String tabId) {
		Preconditions.checkArgument(!tabs.contains(tabId), "Tab is already present: %s", tabId);
		tabs.add(tabId);
		return this;
	}
}
