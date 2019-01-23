/*
 *
 * Essentials-Expansion
 * Copyright (C) 2018 Ryan McCarthy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */
package com.extendedclip.papi.expansion.essentials;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Date;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.util.TimeUtil;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.Kit;
import com.earth2me.essentials.User;

public class EssentialsExpansion extends PlaceholderExpansion {

	private Essentials essentials;
	
	private final String VERSION = getClass().getPackage().getImplementationVersion();
	
	@Override
	public boolean canRegister() {
		return Bukkit.getPluginManager().getPlugin("Essentials") != null;
	}
	
	@Override
	public boolean register() {
		essentials = (Essentials) Bukkit.getPluginManager().getPlugin(getPlugin());
		if (essentials != null) {
			return super.register();
		}
		return false;
	}

	@Override
	public String getAuthor() {
		return "clip";
	}

	@Override
	public String getIdentifier() {
		return "essentials";
	}

	@Override
	public String getPlugin() {
		return "Essentials";
	}

	@Override
	public String getVersion() {
		return VERSION;
	}

	@Override
	public String onPlaceholderRequest(Player p, String identifier) {



		if (p == null) return "";
		
		if (identifier.startsWith("kit_last_use_")) {
			String kit = identifier.split("kit_last_use_")[1];
			
			Kit k = null;
			
			try {
				k = new Kit(kit, essentials);
			} catch (Exception e) {
				return "invalid kit";
			}
			
			long time = essentials.getUser(p).getKitTimestamp(k.getName());
			
			if (time == 1 || time <= 0) {
				return "1";
			}
			return PlaceholderAPIPlugin.getDateFormat().format(new Date(time));
		}
		
		if (identifier.startsWith("kit_is_available_")) {
			String kit = identifier.split("kit_is_available_")[1];
			
			Kit k = null;
			
			User u = essentials.getUser(p);
			
			try {
				k = new Kit(kit, essentials);
			} catch (Exception e) {
				return PlaceholderAPIPlugin.booleanFalse();
			}
			
			long time = -1   ;
			
			try {
				time = k.getNextUse(u);
			} catch (Exception e) {
				return PlaceholderAPIPlugin.booleanFalse();
			}
			
			return time == 0 ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
		}
		
		if (identifier.startsWith("kit_time_until_available_")) {
			String kit = identifier.split("kit_time_until_available_")[1];
			
			Kit k = null;
			
			User u = essentials.getUser(p);
			
			try {
				k = new Kit(kit, essentials);
			} catch (Exception e) {
				return PlaceholderAPIPlugin.booleanFalse();
			}
			
			long time = -1;
			
			try {
				time = k.getNextUse(u);
			} catch (Exception e) {
				return "-1";
			}
			int seconds = (int)(time - System.currentTimeMillis())/1000;
			
			if (seconds <= 0) {
				return "0";
			}
			return TimeUtil.getTime(seconds);
		}
		
		if (identifier.startsWith("has_kit_")) {
			String kit = identifier.split("has_kit_")[1];
			return p.hasPermission("essentials.kits." + kit) ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
		}
		
		switch (identifier) {
		case "is_pay_confirm":
			return essentials.getUser(p).isPromptingPayConfirm() ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
		case "is_pay_enabled":
			return essentials.getUser(p).isAcceptingPay() ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
		case "is_teleport_enabled":
			return essentials.getUser(p).isTeleportEnabled() ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
        case "is_muted":
            return essentials.getUser(p).isMuted() ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
		case "vanished":
			return essentials.getUser(p).isVanished() ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
		case "afk":
			return essentials.getUser(p).isAfk() ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
        case "msg_ignore":
            return essentials.getUser(p).isIgnoreMsg() ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
        case "fly":
			return essentials.getUser(p).isFlyClickJump() ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
		case "nickname":
			return essentials.getUser(p).getNickname() != null ? essentials.getUser(p).getNickname() : p.getName();
		case "godmode":
			return essentials.getUser(p).isGodModeEnabled() ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
		case "unique":
			return NumberFormat.getInstance().format(essentials.getUserMap().getUniqueUsers());
		case "homes_set":
            return essentials.getUser(p).getHomes().size() == 0 ? String.valueOf(0) : String.valueOf(essentials.getUser(p).getHomes().size());
        case "homes_max":
            return String.valueOf(essentials.getSettings().getHomeLimit(essentials.getUser(p)));
		case "jailed":
			return String.valueOf(essentials.getUser(p).isJailed());
		case "pm_recipient":
			User u = essentials.getUser(p);
			return u.getReplyRecipient() != null ? u.getReplyRecipient().getName() : "";
		case "safe_online":
			return String.valueOf((essentials.getOnlinePlayers().size() - essentials.getVanishedPlayers().size()));
		case "worth":
			if (p.getItemInHand() == null || p.getItemInHand().getType() == Material.AIR) return "";
			BigDecimal worth = essentials.getWorth().getPrice(null, p.getItemInHand());
			if (worth == null) return "";
			return String.valueOf(worth.doubleValue());
		}
		return null;
	}
	
	
	
	
}
