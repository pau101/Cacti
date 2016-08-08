package com.pau101.cacti.api;

/**
 * This class supplies the root categories instance for specifying
 * mod categories.
 */
public final class CactiAPI {
	/**
	 * Prevent this class from being instantiated.
	 */
	private CactiAPI() {}

	/**
	 * The root categories instance.
	 */
	private static final CactiEntryCategory CATEGORIES = new CactiEntryCategory("categoryEntry", null)
		.withTabGroup("minecraft")
		.end();

	/**
	 * Returns the instance of the root categories
	 * object.
	 *
	 * @return the root categories instance
	 */
	public static CactiEntryCategory categories() {
		return CATEGORIES;
	}
}
