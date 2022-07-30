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

package io.github.moulberry.notenoughupdates.miscgui.minionhelper.loaders.repo;

import com.google.common.collect.ArrayListMultimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.util.StringUtils;
import io.github.moulberry.notenoughupdates.events.RepositoryReloadEvent;
import io.github.moulberry.notenoughupdates.miscgui.minionhelper.Minion;
import io.github.moulberry.notenoughupdates.miscgui.minionhelper.MinionHelperManager;
import io.github.moulberry.notenoughupdates.miscgui.minionhelper.requirements.CustomRequirement;
import io.github.moulberry.notenoughupdates.miscgui.minionhelper.sources.CustomSource;
import io.github.moulberry.notenoughupdates.miscgui.minionhelper.sources.NpcSource;
import io.github.moulberry.notenoughupdates.util.Constants;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class MinionHelperRepoLoader {
	private final MinionHelperManager manager;
	private boolean dirty = true;
	private int ticks = 0;
	private final Map<String, String> displayNameCache = new HashMap<>();
	private boolean repoReadyToUse = false;
	private final MinionHelperRepoMinionLoader minionLoader;
	boolean errorWhileLoading = false;

	public MinionHelperRepoLoader(MinionHelperManager manager) {
		this.manager = manager;
		minionLoader = new MinionHelperRepoMinionLoader(this, manager);
	}

	/**
	 * This adds support for the /neureloadrepo command
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onRepoReload(RepositoryReloadEvent event) {
		setDirty();
	}

	@SubscribeEvent
	public void onTick(TickEvent.ClientTickEvent event) {
		if (Minecraft.getMinecraft().thePlayer == null) return;
		if (!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()) return;
		ticks++;

		if (ticks % 5 != 0) return;

		if (dirty) {
			dirty = false;
			load();
		}
	}

	void load() {
		errorWhileLoading = false;

		createMinions();

		loadNpcData();
		minionLoader.loadMinionData();
		loadCustomSources();

		testForMissingData();

		manager.reloadData();
		repoReadyToUse = true;

		if (errorWhileLoading) {
			Utils.showOutdatedRepoNotification();
		}
	}

	private void loadCustomSources() {
		Map<String, String> customSource = new HashMap<>();

		customSource.put("SNOW_GENERATOR_1", "Gifts");

		customSource.put("FLOWER_GENERATOR_1", "Dark Auction");

		customSource.put("REVENANT_GENERATOR_1", "Zombie Slayer");
		customSource.put("TARANTULA_GENERATOR_1", "Spider Slayer");
		customSource.put("VOIDLING_GENERATOR_1", "Enderman Slayer");
		customSource.put("INFERNO_GENERATOR_1", "Blaze Slayer");

		for (Map.Entry<String, String> entry : customSource.entrySet()) {
			String internalName = entry.getKey();
			String sourceName = entry.getValue();
			Minion minion = manager.getMinionById(internalName);
			if (minion == null) continue;
			minion.setMinionSource(new CustomSource(minion, sourceName));
		}

		manager.getMinionById("FLOWER_GENERATOR_1").getRequirements().add(new CustomRequirement(
			"Buy a Flower Minion 1 from Dark Auction"));
		manager.getMinionById("SNOW_GENERATOR_1").getRequirements().add(new CustomRequirement(
			"Get a Snow Minion 1 from opening gifts"));

	}

	private void loadNpcData() {
		TreeMap<String, JsonObject> itemInformation = NotEnoughUpdates.INSTANCE.manager.getItemInformation();
		for (Map.Entry<String, JsonObject> entry : itemInformation.entrySet()) {
			String internalName = entry.getKey();
			if (!internalName.endsWith("_NPC")) continue;
			JsonObject jsonObject = entry.getValue();
			if (!jsonObject.has("recipes")) continue;

			if (!jsonObject.has("displayname")) continue;
			String npcName = jsonObject.get("displayname").getAsString();
			npcName = StringUtils.cleanColour(npcName);
			if (npcName.contains(" (")) {
				npcName = npcName.split(" \\(")[0];
			}

			for (JsonElement element : jsonObject.get("recipes").getAsJsonArray()) {
				JsonObject object = element.getAsJsonObject();
				if (!object.has("type")) continue;
				if (!object.get("type").getAsString().equals("npc_shop")) continue;
				if (!object.has("result")) continue;

				String result = object.get("result").getAsString();
				if (!result.contains("_GENERATOR_")) continue;
				Minion minion = manager.getMinionById(result);
				if (!object.has("cost")) continue;

				RecipeBuilder builder = new RecipeBuilder(manager);

				for (JsonElement costEntry : object.get("cost").getAsJsonArray()) {
					String price = costEntry.getAsString();
					builder.addLine(minion, price);
				}

				ArrayListMultimap<String, Integer> map = builder.getItems();
				int coins = 0;
				if (map.containsKey("SKYBLOCK_COIN")) {
					coins = map.get("SKYBLOCK_COIN").get(0);
					map.removeAll("SKYBLOCK_COIN");
				}

				minion.setMinionSource(new NpcSource(minion, npcName, coins, builder.getItems()));
				minion.setParent(builder.getParent());
			}
		}
	}

	private void createMinions() {
		for (Map.Entry<String, JsonElement> entry : Constants.MISC.get("minions").getAsJsonObject().entrySet()) {
			String internalName = entry.getKey();
			int maxTier = entry.getValue().getAsInt();
			for (int i = 0; i < maxTier; i++) {
				int tier = i + 1;
				manager.createMinion(internalName + "_" + tier, tier);
			}
		}
	}

	private void testForMissingData() {
		for (Minion minion : manager.getAllMinions().values()) {
			if (minion.getMinionSource() == null) {
				errorWhileLoading = true;
				if (NotEnoughUpdates.INSTANCE.config.hidden.dev) {
					Utils.addChatMessage("§c[NEU] The Minion '" + minion.getInternalName() + " has no source!");
				}
			}
			if (minion.getDisplayName() == null) {
				errorWhileLoading = true;
				if (NotEnoughUpdates.INSTANCE.config.hidden.dev) {
					Utils.addChatMessage("§c[NEU] The Minion '" + minion.getInternalName() + " has no display name!");
				}
			}
			if (manager.getRequirementsManager().getRequirements(minion).isEmpty()) {
				errorWhileLoading = true;
				if (NotEnoughUpdates.INSTANCE.config.hidden.dev) {
					Utils.addChatMessage("§c[NEU] The Minion '" + minion.getInternalName() + " has no requirements!");
				}
			}
			if (minion.getTier() > 1 && minion.getParent() == null) {
				errorWhileLoading = true;
				if (NotEnoughUpdates.INSTANCE.config.hidden.dev) {
					Utils.addChatMessage("§c[NEU] The Minion '" + minion.getInternalName() + " has parent!");
				}
			}
		}
	}

	//TODO move into utils class or somewhere
	public String getDisplayName(String internalName) {
		if (displayNameCache.containsKey(internalName)) {
			return displayNameCache.get(internalName);
		}

		String displayName = null;
		TreeMap<String, JsonObject> itemInformation = NotEnoughUpdates.INSTANCE.manager.getItemInformation();
		if (itemInformation.containsKey(internalName)) {
			JsonObject jsonObject = itemInformation.get(internalName);
			if (jsonObject.has("displayname")) {
				displayName = jsonObject.get("displayname").getAsString();
			}
		}

		if (displayName == null) {
			displayName = internalName;
			Utils.showOutdatedRepoNotification();
			if (NotEnoughUpdates.INSTANCE.config.hidden.dev) {
				Utils.addChatMessage("§c[NEU] Found no display name in repo for '" + internalName + "'!");
			}
		}

		displayNameCache.put(internalName, displayName);
		return displayName;
	}

	public void setDirty() {
		dirty = true;
		displayNameCache.clear();
		repoReadyToUse = false;
	}

	public boolean isRepoReadyToUse() {
		return repoReadyToUse;
	}
}
