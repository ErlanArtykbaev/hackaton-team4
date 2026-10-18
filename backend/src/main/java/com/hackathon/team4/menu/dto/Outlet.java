package com.hackathon.team4.menu.dto;

import com.hackathon.team4.menu.OutletKind;
import java.util.List;

/**
 * A hotel outlet (POS revenue center) with its tables; room service has none.
 */
public record Outlet(int rvc, String name, OutletKind kind, List<String> tables) {
}
