/*
 * Copyright (C) 2022 NotEnoughUpdates contributors
 *
 * This file is part of NotEnoughUpdates.
 *
 * NotEnoughUpdates is free software: you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * NotEnoughUpdates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with NotEnoughUpdates. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.moulberry.notenoughupdates.miscgui.minionhelper;

import java.util.Map;

public class ApiData {

	private final Map<String, Integer> highestCollectionTier;
	private final Map<String, Integer> slayerTiers;
	private final int magesReputation;
	private final int barbariansReputation;
	private final boolean collectionApiDisabled;

	public ApiData(
		Map<String, Integer> highestCollectionTier,
		Map<String, Integer> slayerTiers,
		int magesReputation,
		int barbariansReputation,
		boolean collectionApiDisabled
	) {
		this.highestCollectionTier = highestCollectionTier;
		this.slayerTiers = slayerTiers;
		this.magesReputation = magesReputation;
		this.barbariansReputation = barbariansReputation;
		this.collectionApiDisabled = collectionApiDisabled;
	}

	public Map<String, Integer> getHighestCollectionTier() {
		return highestCollectionTier;
	}

	public Map<String, Integer> getSlayerTiers() {
		return slayerTiers;
	}

	public int getMagesReputation() {
		return magesReputation;
	}

	public int getBarbariansReputation() {
		return barbariansReputation;
	}

	public boolean isCollectionApiDisabled() {
		return collectionApiDisabled;
	}
}
