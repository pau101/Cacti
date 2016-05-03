package com.pau101.cacti.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import net.minecraft.creativetab.CreativeTabs;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * An entry containing sub categories and tab groups.
 */
public final class CactiEntryCategory extends CactiEntry {
	/**
	 * The list of entries contained within this category
	 */
	private final List<CactiEntry> entries;

	/**
	 * A map of entry ids to values contained within {@link #entries}
	 * This is used for conveniently retrieving entries
	 * based on their id.
	 */
	private final Map<String, CactiEntry> entryMap;

	CactiEntryCategory(String id, CactiEntryCategory owner) {
		super(id, owner);
		entries = new ArrayList<CactiEntry>();
		entryMap = new HashMap<String, CactiEntry>();
	}

	/**
	 * Provides an immutable copy of the entries
	 * within this category.
	 *
	 * @return immutable copy of {@link #entries}
	 */
	public ImmutableList<CactiEntry> getEntries() {
		return ImmutableList.copyOf(entries);
	}

	/**
	 * Returns the associated entry to the specified id, or
	 * {@code null} if this category does not contain the specified unique
	 * identifier.
	 *
	 * @param id the unique indentifier whose associated entry is returned
	 * @return the entry associated with the specified id, or
	 *         {@code null} if this category does not contain the specified
	 *         indentifier
	 */
	public CactiEntry get(String id) {
		return entryMap.get(id);
	}

	/**
	 * Returns the associated category to the specified id, or
	 * {@code null} if this category does not contain the specified unique
	 * identifier.
	 *
	 * @param id the unique indentifier whose associated category is returned
	 * @return the category associated with the specified id, or
	 *         {@code null} if this category does not contain the specified
	 *         indentifier
	 * @throws NoSuchElementException If the the entry associated with this
	 *         identifier is not a category
	 */
	public CactiEntryCategory getCategory(String id) {
		CactiEntry entry = entryMap.get(id);
		if (entry instanceof CactiEntryCategory || entry == null) {
			return (CactiEntryCategory) entry;
		}
		throw new NoSuchElementException("Category not present: " + id);
	}

	/**
	 * Returns the associated tab group to the specified id, or
	 * {@code null} if this category does not contain the specified unique
	 * identifier.
	 *
	 * @param id the unique indentifier whose associated tab group is returned
	 * @return the tab group associated with the specified id, or
	 *         {@code null} if this category does not contain the specified
	 *         indentifier
	 * @throws NoSuchElementException If the the entry associated with this
	 *         identifier is not a tab group
	 */
	public CactiEntryTabGroup getTabGroup(String id) {
		CactiEntry entry = entryMap.get(id);
		if (entry instanceof CactiEntryTabGroup || entry == null) {
			return (CactiEntryTabGroup) entry;
		}
		throw new NoSuchElementException("Tab group not present: " + id);
	}

	/**
	 * Returns {@code true} if this category contains an entry with the specified
	 * unique indentifier
	 *
	 * @param id the unique indentifier whose presence in this category is tested
	 * @return {@code true} if this category has an entry with the specified id
	 */
	public boolean hasEntry(String id) {
		return entryMap.containsKey(id);
	}

	/**
	 * Returns {@code true} if this category contains a category with the specified
	 * unique indentifier
	 *
	 * @param id the unique indentifier whose associated category presence in this category is tested
	 * @return {@code true} if this category has a category with the specified id
	 */
	public boolean hasCategory(String id) {
		return entryMap.get(id) instanceof CactiEntryCategory;
	}

	/**
	 * Returns {@code true} if this category contains a tab group with the specified
	 * unique indentifier
	 *
	 * @param id the unique indentifier whose associated tab group presence in this category is tested
	 * @return {@code true} if this category has a tab group with the specified id
	 */
	public boolean hasTabGroup(String id) {
		return entryMap.get(id) instanceof CactiEntryTabGroup;
	}

	@Override
	public boolean contains(CreativeTabs tab) {
		if (tab == null) {
			return false;
		}
		for (CactiEntry entry : entries) {
			if (entry.contains(tab)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Adds a new subcategory to this category with the specified unique indentifier.
	 *
	 * @param id the unique indentifier to construct the category with
	 * @return the created category
	 * @throws IllegalArgumentException If this category already contains an entry with
	 *         {@code id}
	 */
	public CactiEntryCategory withCategory(String id) {
		Preconditions.checkArgument(!entryMap.containsKey(id), "The specified id is already present in this category: %s", id);
		CactiEntryCategory category = new CactiEntryCategory(id, this);
		entries.add(category);
		entryMap.put(id, category);
		return category;
	}

	/**
	 * Adds a new tab group to this category with the specified unique indentifier.
	 *
	 * @param id the unique indentifier to construct the category with
	 * @return the created category
	 * @throws IllegalArgumentException If this category already contains an entry with
	 *         {@code id}
	 */
	public CactiEntryTabGroup withTabGroup(String id) {
		Preconditions.checkArgument(!entryMap.containsKey(id), "The specified id is already present in this category: %s", id);
		CactiEntryTabGroup category = new CactiEntryTabGroup(id, this);
		entries.add(category);
		entryMap.put(id, category);
		return category;
	}
}
