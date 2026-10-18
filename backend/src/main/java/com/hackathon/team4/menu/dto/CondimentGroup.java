package com.hackathon.team4.menu.dto;

import java.util.List;

/**
 * Condiments of a menu item (e.g. milk for a coffee); the waiter picks between minSelect and maxSelect options.
 */
public record CondimentGroup(String code, String name, int minSelect, int maxSelect, List<CondimentOption> options) {
}
