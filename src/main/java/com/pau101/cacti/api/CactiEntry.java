package com.pau101.cacti.api;

import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * A labeled entry representable in the creative inventory.
 */
public abstract class CactiEntry implements Comparable<CactiEntry> {
	/**
	 * The unique identifier for this entry.
	 */
	private final String id;

	/**
	 * The owner of this category.
	 */
	private final CactiEntryCategory owner;

	/**
	 * The translation key for the displayed name of this entry.
	 */
	private String unlocalizedNameKey;

	/**
	 * The override display name.
	 */
	private String customName;

	/**
	 * Creates an entry with the specified unique identifier and
	 * owner category.
	 *
	 * <p>
	 * The {@link #unlocalizedNameKey} is set to {@link #createUnlocalizedNameKey()}
	 * </p>
	 *
	 * @param id the unique indentifier for this entry
	 * @param owner the category which contains this category
	 * @throws IllegalArgumentException If {@code id} is null or empty
	 */
	CactiEntry(String id, CactiEntryCategory owner) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(id), "id must be non-null and not empty");
		this.id = id;
		this.owner = owner;
		unlocalizedNameKey = createUnlocalizedNameKey();
	}

	/**
	 * Returns the unlocalized name key.
	 *
	 * The key is comprised of the concatenation of all the parent categories
	 * and this entry's id with a joining character of ".", where the root
	 * parent is {@link CactiAPI#categories()} with the id of "categoryEntry".
	 *
	 * @return
	 */
	protected final String createUnlocalizedNameKey() {
		return owner == null ? getId() : (owner.createUnlocalizedNameKey() + "." + getId());
	}

	/**
	 * Assigns the translation key to be used for localizing the name
	 * displayed in the creative inventory.
	 *
	 * @param unlocalizedNameKey the translation key to be used for display
	 *        name localization
	 * @throws IllegalArgumentException If {@code unlocalizedNameKey} is null or empty
	 */
	public final void setUnlocalizedNameKey(String unlocalizedNameKey) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(unlocalizedNameKey), "unlocalizedNameKey must be non-null and not empty");
		this.unlocalizedNameKey = unlocalizedNameKey;
	}

	/**
	 * Configures this entry to display the specified string instead of a
	 * localized value.
	 *
	 * @param customName the String to be displayed in place of a localized
	 *        name
	 * @throws IllegalArgumentException If {@code customName} is null
	 */
	public final void setCustomName(String customName) {
		Preconditions.checkArgument(customName != null, "customName must be non-null");
		this.customName = customName;
	}

	/**
	 * Returns the unique identifier of this entry.
	 *
	 * @return the id of this entry
	 */
	public final String getId() {
		return id;
	}

	/**
	 * Returns the category which this entry is contained within.
	 *
	 * @return the owner of this entry
	 */
	public final CactiEntryCategory getOwner() {
		return owner;
	}

	/**
	 * Returns the name of this entry to be displayed
	 * in the creative inventory.
	 * If {@link #customName} is non-null then it is returned,
	 * otherwise the localized {@link #unlocalizedNameKey} key will be.
	 *
	 * @return the String to be displayed for this entry
	 *         in the creative inventory
	 */
	public final String getDisplayName() {
		if (customName == null) {
			return I18n.format(unlocalizedNameKey);
		}
		return customName;
	}

	/**
	 * Compares two entries lexicographically by {@link #getDisplayName()}.
	 *
	 * @see java.lang.String#compareTo(String)
	 */
	@Override
	public final int compareTo(CactiEntry other) {
		return getDisplayName().compareTo(other.getDisplayName());
	}

	/**
	 * Returns {@code true} if the specified CreativeTabs is
	 * contained within this entry.
	 *
	 * @param tab the CreativeTabs whose presence in this entry
	 *        is to be tested
	 * @return {@code true} if this entry contains the specified
	 *         CreativeTabs
	 */
	public abstract boolean contains(CreativeTabs tab);

	/**
	 * Returns the category containing this entry for use in ending
	 * this category constructed with the "Builder" pattern methods
	 * present in this class's derived classes.  
	 * Functionally identical to {@link #getOwner()}.
	 *
	 * @return the owner of this entry
	 * @see CactiEntryCategory#withCategory
	 * @see CactiEntryCategory#withTabGroup
	 * @see CactiEntryTabGroup#withTab
	 */
	public final CactiEntryCategory end() {
		return owner;
	}
}
