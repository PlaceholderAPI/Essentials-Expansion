/*
 *
 * Essentials-Expansion
 * Copyright (C) 2019 Ryan McCarthy
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

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.Kit;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.DateUtil;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.stream.StreamSupport;

public class EssentialsExpansion extends PlaceholderExpansion {

    private Essentials essentials;

    private final String VERSION = getClass().getPackage().getImplementationVersion();

    @Override
    public boolean canRegister() {
        return Bukkit.getPluginManager().getPlugin("Essentials") != null;
    }

    @Override
    public boolean register() {
        essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        if (essentials != null) {
            return super.register();
        }
        return false;
    }

    @Override
    public @NotNull String getAuthor() {
        return "clip";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "essentials";
    }

    @Override
    public @NotNull String getVersion() {
        return VERSION;
    }

    @Override
    public String onRequest(OfflinePlayer givenPlayer, @NotNull String identifier) {
        final Player player = (Player) givenPlayer;

        if (givenPlayer == null) return "";

        if (identifier.startsWith("kit_last_use_")) {
            String kitName = identifier.split("kit_last_use_")[1].toLowerCase();
            Kit kit;

            try {
                kit = new Kit(kitName, essentials);
            } catch (Exception e) {
                return "Invalid kit name";
            }

            long time = essentials.getUser(player.getUniqueId()).getKitTimestamp(kit.getName());

            if (time == 1 || time <= 0) {
                return "1";
            }
            return PlaceholderAPIPlugin.getDateFormat().format(new Date(time));
        }

        if (identifier.startsWith("kit_is_available_")) {
            String kitName = identifier.split("kit_is_available_")[1].toLowerCase();
            Kit kit;
            User user = essentials.getUser(givenPlayer.getUniqueId());
            long time;

            try {
                kit = new Kit(kitName, essentials);
            } catch (Exception e) {
                return "Invalid kit name";
            }

            try {
                time = kit.getNextUse(user);
            } catch (Exception e) {
                return PlaceholderAPIPlugin.booleanFalse();
            }

            return time == 0 ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
        }

        if (identifier.startsWith("kit_time_until_available_")) {
            String kitName = identifier.split("kit_time_until_available_")[1].toLowerCase();
            boolean raw = false;
            User user = essentials.getUser(givenPlayer.getUniqueId());
            Kit kit;
            long time;

            if (kitName.startsWith("raw_")) {
                raw = true;
                kitName = kitName.substring(4);

                if (kitName.isEmpty()) {
                    return "Invalid kit name";
                }
            }

            try {
                kit = new Kit(kitName, essentials);
            } catch (Exception e) {
                return "Invalid kit name";
            }

            try {
                time = kit.getNextUse(user);
            } catch (Exception e) {
                return "-1";
            }

            if (time <= System.currentTimeMillis()) {
                return raw ? "0" : DateUtil.formatDateDiff(System.currentTimeMillis());
            }

            if (raw) {
                return String.valueOf(Instant.now().until(Instant.ofEpochMilli(time), ChronoUnit.MILLIS));
            } else {
                return DateUtil.formatDateDiff(time);
            }
        }

        if (identifier.startsWith("has_kit_")) {
            String kit = identifier.split("has_kit_")[1];
            return player.hasPermission("essentials.kits." + kit) ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
        }

        if (identifier.startsWith("worth")) {
            ItemStack item;

            if (identifier.contains(":")){
                Material material = Material.getMaterial(identifier.replace("worth:", "").toUpperCase());

                if (material == null) return "";
                item = new ItemStack(material,1);
            } else {
                if (player.getItemInHand().getType() == Material.AIR) return "";
                item = player.getItemInHand();
            }

            BigDecimal worth = essentials.getWorth().getPrice(essentials, item);
            if (worth == null) return "";
            return String.valueOf(worth.doubleValue());
        }

        final User user = essentials.getUser(givenPlayer.getUniqueId());

        switch (identifier) {
            case "is_pay_confirm":
                return user.isPromptingPayConfirm() ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
            case "is_pay_enabled":
                return user.isAcceptingPay() ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
            case "is_teleport_enabled":
                return user.isTeleportEnabled() ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
            case "is_muted":
                return user.isMuted() ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
            case "vanished":
                return user.isVanished() ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
            case "afk":
                return user.isAfk() ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
            case "afk_reason":
                if (user.getAfkMessage() == null) return "";
                return ChatColor.translateAlternateColorCodes('&', user.getAfkMessage());
            case "afk_player_count":
                return String.valueOf(essentials.getUserMap().getAllUniqueUsers().stream()
                        .map(UUID -> essentials.getUser(UUID)).filter(User::isAfk)
                        .count());
            case "msg_ignore":
                return user.isIgnoreMsg() ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
            case "fly":
                return user.getBase().getAllowFlight() ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
            case "nickname":
                return user.getNickname() != null ? essentials.getUser(givenPlayer.getUniqueId()).getNickname() : givenPlayer.getName();
            case "godmode":
                return user.isGodModeEnabled() ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
            case "unique":
                return NumberFormat.getInstance().format(essentials.getUserMap().getUniqueUsers());
            case "homes_set":
                return user.getHomes().isEmpty() ? "0" : String.valueOf(user.getHomes().size());
            case "homes_max":
                return String.valueOf(essentials.getSettings().getHomeLimit(user));
            case "jailed":
                return String.valueOf(user.isJailed());
            case "jailed_time_remaining":
                return user.getFormattedJailTime();
            case "pm_recipient":
                return user.getReplyRecipient() != null ? user.getReplyRecipient().getName() : "";
            case "safe_online":
                return String.valueOf(StreamSupport.stream(essentials.getOnlineUsers().spliterator(), false)
                        .filter(user1 -> !user1.isHidden())
                        .count());
        }
        return null;
    }


}
