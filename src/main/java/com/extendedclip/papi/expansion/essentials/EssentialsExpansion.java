/*
 *
 * Essentials-Expansion
 * Copyright (C) 2020 Ryan McCarthy
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
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.Kit;
import com.earth2me.essentials.User;

public class EssentialsExpansion extends PlaceholderExpansion {

    private Essentials essentials;

    private final String VERSION = getClass().getPackage().getImplementationVersion();
    private final boolean booleanTrue = PlaceholderAPIPlugin.booleanTrue();
    private final boolean booleanFalse = PlaceholderAPIPlugin.booleanFalse();

    @Override
    public boolean canRegister() {
        return Bukkit.getPluginManager().getPlugin("Essentials") != null;
    }

    @Override
    public boolean register() {
        essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");

        if (essentials != null) return super.register();

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
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String onPlaceholderRequest(Player p, String identifier) {
        if (p == null) return "";

        if (identifier.startsWith("kit_last_use_")) {
            String kit = identifier.split("kit_last_use_")[1].toLowerCase();

            Kit k = null;

            try {
                k = new Kit(kit, essentials);
            } catch (Exception e) {
                return "invalid kit";
            }

            long time = essentials.getUser(p).getKitTimestamp(k.getName());

            return (time == 1 || time <= 0) ? "1" : PlaceholderAPIPlugin.getDateFormat().format(new Date(time));
        }

        if (identifier.startsWith("kit_is_available_")) {
            String kit = identifier.split("kit_is_available_")[1].toLowerCase();

            Kit k = null;

            User u = essentials.getUser(p);

            try {
                k = new Kit(kit, essentials);
            } catch (Exception e) {
                return booleanFalse;
            }

            long time = -1;

            try {
                time = k.getNextUse(u);
            } catch (Exception e) {
                return booleanFalse;
            }

            return time == 0 ? booleanTrue : booleanFalse;;
        }

        if (identifier.startsWith("kit_time_until_available_")) {
            String kit = identifier.split("kit_time_until_available_")[1].toLowerCase();

            Kit k = null;

            User u = essentials.getUser(p);

            try {
                k = new Kit(kit, essentials);
            } catch (Exception e) {
                return booleanFalse;
            }

            long time = -1;

            try {
                time = k.getNextUse(u);
            } catch (Exception e) {
                return "-1";
            }

            int seconds = (int) (time - System.currentTimeMillis()) / 1000;

            return (seconds <= 0 || time == 0) ? "0" : TimeUtil.getTime(seconds);
        }

        if (identifier.startsWith("has_kit_")) {
            String kit = identifier.split("has_kit_")[1];
		
            return p.hasPermission("essentials.kits." + kit) ? booleanTrue : booleanFalse;;
        }

        switch (identifier) {
            case "is_pay_confirm":
                return essentials.getUser(p).isPromptingPayConfirm() ? booleanTrue : booleanFalse;;
            case "is_pay_enabled":
                return essentials.getUser(p).isAcceptingPay() ? booleanTrue : booleanFalse;;
            case "is_teleport_enabled":
                return essentials.getUser(p).isTeleportEnabled() ? booleanTrue : booleanFalse;;
            case "is_muted":
                return essentials.getUser(p).isMuted() ? booleanTrue : booleanFalse;;
            case "vanished":
                return essentials.getUser(p).isVanished() ? booleanTrue : booleanFalse;;
            case "afk":
                return essentials.getUser(p).isAfk() ? booleanTrue : booleanFalse;;
            case "afk_reason":
                return essentials.getUser(p).getAfkMessage() == null ? null : ChatColor.translateAlternateColorCodes('&', essentials.getUser(p).getAfkMessage());
            case "msg_ignore":
                return essentials.getUser(p).isIgnoreMsg() ? booleanTrue : booleanFalse;;
            case "fly":
                return essentials.getUser(p).isFlyClickJump() ? booleanTrue : booleanFalse;;
            case "nickname":
                return essentials.getUser(p).getNickname() != null ? essentials.getUser(p).getNickname() : p.getName();
            case "godmode":
                return essentials.getUser(p).isGodModeEnabled() ? booleanTrue : booleanFalse;;
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
                int playerHidden = 0;

                for (User onlinePlayer : essentials.getOnlineUsers()) {
                    if (onlinePlayer.isHidden()) {
                        playerHidden++;
                    }
                }
                return String.valueOf((essentials.getOnlinePlayers().size() - playerHidden));
            case "worth":
                if (p.getItemInHand() == null || p.getItemInHand().getType() == Material.AIR) return "";
			
                BigDecimal worth = essentials.getWorth().getPrice(null, p.getItemInHand());

                return worth == null ? null : String.valueOf(worth.doubleValue());
        }
    }
}
