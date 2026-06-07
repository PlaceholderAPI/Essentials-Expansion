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
package at.helpch.essentialsexpansion;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.Kit;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.DateUtil;
import com.earth2me.essentials.utils.DescParseTickFormat;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import com.google.common.primitives.Ints;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Taskable;
import net.essentialsx.api.v2.services.BalanceTop;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class EssentialsExpansion extends PlaceholderExpansion implements Taskable, Configurable {

    private static final NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
    private static final DecimalFormat commasFormat = new DecimalFormat("#,###");
    private static final DecimalFormat coordsFormat = commasFormat;
    private static final List<String> placeholders;

    static {
        placeholders = Stream.of(
                Stream.of(
                        "balance",
                        "balance_commas",
                        "balance_fixed",
                        "balance_formatted",
                        "player_stripped",
                        "player"
                ).map(placeholder -> "baltop_" + placeholder + "_<rank>"),

                Stream.of(
                        "baltop_rank",
                        "is_clearinventory_confirm",
                        "is_pay_confirm",
                        "is_pay_enabled",
                        "is_private_messages_enabled",
                        "is_teleport_auto_accept_enabled",
                        "is_teleport_enabled",
                        "muted",
                        "vanished",

                        "afk",
                        "afk_reason",
                        "afk_player_count",

                        "msg_ignore",
                        "fly",

                        "nickname",
                        "nickname_stripped",

                        "muted_time_remaining",
                        "geolocation",
                        "godmode",
                        "unique",

                        "jailed",
                        "jailed_time_remaining",

                        "pm_recipient",
                        "safe_online",
                        "tp_cooldown",

                        "world_date",
                        "world_time",
                        "world_time_24",

                        "worth",
                        "worth_<material>"
                ),

                Stream.of(
                        "last_use",
                        "is_available",
                        "time_until_available",
                        "has"
                ).map(placeholder -> "kit_" + placeholder + "_<kit>"),

                Stream.of(
                        Stream.of(
                                "total",
                                "max",
                                "name_<index>",
                                "has_<name>"
                        ),
                        Stream.of(
                                "world",
                                "x",
                                "y",
                                "z"
                        ).map(placeholder -> placeholder + "_<name|index>")
                ).flatMap(s->s).map(placeholder -> "home_" + placeholder)

        ).flatMap(s->s).map(placeholder -> "%essentials_" + placeholder + "%").toList();
    }

    private Map<Long, String> formats;

    private Essentials essentials;
    private BalanceTop baltop;
    private BukkitTask refreshTask = null;

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
        return "2.0.0";
    }

    @Override
    public @NotNull String getRequiredPlugin() {
        return "Essentials";
    }

    @Override
    public @NotNull List<String> getPlaceholders() {
        return placeholders;
    }

    @Override
    public Map<String, Object> getDefaults() {
        return Map.of(
                "formatting.thousands", "k",
                "formatting.millions", "m",
                "formatting.billions", "b",
                "formatting.trillions", "t",
                "formatting.quadrillions", "q",
                "baltop.refresh", 5L
        );
    }

    @Override
    public void start() {
        formats = new LinkedHashMap<>() {{
            put(1000000000000000L, getString("formatting.quadrillions", "q"));
            put(1000000000000L, getString("formatting.trillions", "t"));
            put(1000000000L, getString("formatting.billions", "b"));
            put(1000000L, getString("formatting.millions", "m"));
            put(1000L, getString("formatting.thousands", "k"));
        }};
        essentials = (Essentials) getPlaceholderAPI().getServer().getPluginManager().getPlugin("Essentials");
        assert essentials != null;
        baltop = essentials.getBalanceTop();
        long refresh = getLong("baltop.refresh", 5);
        if (refresh > 0) {
            refreshTask = getPlaceholderAPI().getServer().getScheduler().runTaskTimerAsynchronously(getPlaceholderAPI(), () -> baltop.calculateBalanceTopMapAsync(), 0, refresh * 20L);
        }
    }

    @Override
    public void stop() {
        if (refreshTask != null) refreshTask.cancel();
        refreshTask = null;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {

        // Put this before the null check as most of it doesn't require a player
        if (params.startsWith("baltop_")) {
            Map<UUID, BalanceTop.Entry> baltopCache = baltop.getBalanceTopCache();
            params = params.substring("baltop_".length());

            String[] args = params.split("_");
            return switch (args[0]) {
                case "balance", "player" -> {
                    if (args.length == 1) yield null;

                    Integer rank = Ints.tryParse(args[args.length > 2 ? 2 : 1]);
                    if (rank == null) yield "Invalid rank";

                    BalanceTop.Entry[] entries = baltopCache.values().toArray(new BalanceTop.Entry[0]);
                    if (rank >= entries.length) yield "0";

                    if (args[0].equals("balance")) {
                        yield switch (args[1]) {
                            case "fixed" -> String.valueOf(entries[rank].getBalance().longValue());
                            case "formatted" -> fixMoney(entries[rank].getBalance().doubleValue());
                            case "commas" -> commasFormat.format(entries[rank].getBalance().doubleValue());
                            default -> String.valueOf(entries[rank].getBalance().doubleValue());
                        };
                    }

                    if (args[1].equals("stripped")) {
                        User user = essentials.getUser(entries[rank].getUuid());
                        yield user != null ? user.getName() : null;
                    }
                    yield entries[rank].getDisplayName();
                }
                case "rank" -> {
                    // Another null check because it's above the normal one
                    if (player == null) yield "";

                    if (!baltopCache.containsKey(player.getUniqueId())) yield "";

                    yield String.valueOf(new ArrayList<>(baltopCache.keySet()).indexOf(player.getUniqueId()) + 1);
                }
                default -> null;
            };
        }

        if (player == null) return "";

        Object output = request(player, params);
        return switch (output) {
            case null -> null;
            case Boolean bool -> bool ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
            default -> output.toString();
        };
    }

    private Object request(OfflinePlayer player, String params) {
        User user = essentials.getUser(player.getUniqueId());
        return switch (params) {
            case "is_clearinventory_confirm" -> user.isPromptingClearConfirm();
            case "is_pay_confirm" -> user.isPromptingPayConfirm();
            case "is_pay_enabled" -> user.isAcceptingPay();
            case "is_teleport_auto_accept_enabled" -> user.isAutoTeleportEnabled();
            case "is_teleport_enabled" -> user.isTeleportEnabled();
            case "is_private_messages_enabled" -> !user.isIgnoreMsg();
            case "muted", "is_muted" -> user.isMuted();
            case "vanished" -> user.isVanished();

            case "afk" -> user.isAfk();
            case "afk_reason" -> user.getAfkMessage() == null ? "" : ChatColor.translateAlternateColorCodes('&', user.getAfkMessage());
            case "afk_player_count" -> String.valueOf(essentials
                    .getUsers()
                    .getAllUserUUIDs()
                    .stream()
                    .map(essentials::getUser)
                    .filter(User::isAfk)
                    .count());

            case "msg_ignore" -> user.isIgnoreMsg();
            case "fly" -> user.getBase().getAllowFlight();

            case "nickname" -> user.getNickname() != null ? user.getNickname() : player.getName();
            case "nickname_stripped" -> ChatColor.stripColor(user.getNickname() != null ? user.getNickname() : player.getName());

            case "muted_time_remaining" -> user.isMuted() ? DateUtil.formatDateDiff(user.getMuteTimeout()) : "";
            case "geolocation" -> user.getGeoLocation() != null ? user.getGeoLocation() : "";
            case "godmode" -> user.isGodModeEnabled();
            case "unique" -> NumberFormat.getInstance().format(essentials.getUsers().getUserCount());

            case "jailed" -> user.isJailed();
            case "jailed_time_remaining" -> user.isJailed() ? user.getFormattedJailTime() : "";

            case "pm_recipient" -> user.getReplyRecipient() != null ? user.getReplyRecipient().getName() : "";
            case "safe_online" -> String.valueOf(StreamSupport
                    .stream(essentials.getOnlineUsers().spliterator(), false)
                    .filter(user1 -> !user1.isHidden())
                    .count());
            case "tp_cooldown" -> {
                final double cooldown = essentials.getSettings().getTeleportCooldown();

                final long now = System.currentTimeMillis();
                final long lastTeleport = user.getLastTeleportTimestamp();

                long diff = TimeUnit.MILLISECONDS.toSeconds(now - lastTeleport);

                yield diff < cooldown ? String.valueOf((int) (cooldown - diff)) :"0";
            }

            case "world_date" -> DateFormat.getDateInstance(DateFormat.MEDIUM, essentials.getI18n().getCurrentLocale())
                    .format(DescParseTickFormat.ticksToDate(user.getWorld() == null ? 0 : user.getWorld().getFullTime()));
            case "world_time" -> DescParseTickFormat.format12(user.getWorld() == null ? 0 : user.getWorld().getTime());
            case "world_time_24" -> DescParseTickFormat.format24(user.getWorld() == null ? 0 : user.getWorld().getTime());
            default -> {
                String[] args = params.split("_");
                params = params.substring(args[0].length() + (params.contains("_") ? 1 : 0));

                yield switch (args[0]) {
                    case "kit" -> {
                        if (params.startsWith("last_use_")) {
                            String kitName = params.substring("last_use_".length()).toLowerCase();

                            try {
                                Kit kit = new Kit(kitName, essentials);
                                long time = user.getKitTimestamp(kit.getName());
                                yield time <= 0 ? "0" : PlaceholderAPIPlugin.getDateFormat().format(new Date(time));
                            } catch (Exception e) {
                                yield  "Invalid kit name";
                            }
                        }

                        if (params.startsWith("is_available_")) {
                            String kitName = params.substring("is_available_".length()).toLowerCase();

                            try {
                                Kit kit = new Kit(kitName, essentials);
                                try {
                                    long time = kit.getNextUse(user);
                                    yield time == 0;
                                } catch (Exception e) {
                                    yield false;
                                }
                            } catch (Exception e) {
                                yield "Invalid kit name";
                            }
                        }
                        if (params.startsWith("time_until_available_")) {
                            String kitName = params.substring("time_until_available_".length()).toLowerCase();
                            boolean raw = false;

                            if (kitName.startsWith("raw_")) {
                                raw = true;
                                kitName = kitName.substring(4);

                            }
                            if (kitName.isEmpty()) yield "Invalid kit name";

                            try {
                                Kit kit = new Kit(kitName, essentials);
                                try {
                                    long time = kit.getNextUse(user);
                                    if (time <= System.currentTimeMillis()) yield raw ? "0" : DateUtil.formatDateDiff(System.currentTimeMillis());
                                    yield raw ? String.valueOf(Instant.now().until(Instant.ofEpochMilli(time), ChronoUnit.MILLIS)) : DateUtil.formatDateDiff(time);
                                } catch (Exception e) {
                                    yield "-1";
                                }
                            } catch (Exception e) {
                                yield "Invalid kit name";
                            }
                        }

                        if (params.startsWith("has_")) {
                            Player p = player.getPlayer();
                            yield p != null && p.hasPermission("essentials.kits." + params.substring("has_".length()));
                        }
                        yield null;
                    }
                    case "home" -> {
                        String type = args[1];

                        List<String> homes = user.getHomes();

                        String homeName = params.substring(type.length() + (params.contains("_") ? 1 : 0));
                        Integer homeNumber = Ints.tryParse(homeName);

                        yield switch (type) {
                            case "total" -> homes.size() + "";
                            case "max" -> essentials.getSettings().getHomeLimit(user);
                            case "has" -> user.hasHome(homeName);
                            case "name" -> homeNumber == null || homeNumber < 0 || homeNumber >= homes.size() ? "" : user.getHomes().get(homeNumber);
                            default -> {
                                if (homeNumber != null && (homeNumber < 0 || homeNumber >= user.getHomes().size())) yield "Invalid home";

                                Location home = user.getHome(homeNumber == null ? homeName : user.getHomes().get(homeNumber));
                                if (home == null) yield "Invalid home";

                                Object output = switch (type) {
                                    case "w", "world" -> home.getWorld() == null ? "null" : home.getWorld().getName();
                                    case "x" -> home.getX();
                                    case "y" -> home.getY();
                                    case "z" -> home.getZ();
                                    default -> null;
                                };
                                yield output instanceof Double coords ? coordsFormat.format(coords) : output;
                            }
                        };
                    }
                    case "worth" -> {
                        ItemStack item;
                        if (args.length == 1) {
                            Player p = player.getPlayer();
                            if (p == null) yield "Player offline and no material provided";

                            item = p.getInventory().getItemInMainHand();
                        } else {
                            Material material = Material.getMaterial(params.toUpperCase());
                            if (material == null) yield "Invalid material";

                            item = new ItemStack(material);
                        }
                        if (item.getType() == Material.AIR) yield "No worth";

                        BigDecimal worth = essentials.getWorth().getPrice(essentials, item);
                        yield worth == null ? "No worth" : worth.doubleValue();
                    }
                    default -> null;
                };
            }
        };
    }

    private String fixMoney(double d) {
        for (Map.Entry<Long, String> format : formats.entrySet()) {
            if (d < format.getKey()) continue;
            numberFormat.setMaximumFractionDigits(2);
            numberFormat.setMinimumFractionDigits(0);
            return numberFormat.format(d / format.getKey()) + format.getValue();
        }
        return numberFormat.format(d);
    }
}
