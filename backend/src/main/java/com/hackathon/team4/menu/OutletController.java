package com.hackathon.team4.menu;

import com.hackathon.team4.menu.dto.MenuItem;
import com.hackathon.team4.menu.dto.Outlet;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The hotel's outlets and the menu of each, with verdicts for a party.
 */
@RestController
@RequestMapping("/api/outlets")
public class OutletController {

    private final MenuService menus;

    public OutletController(MenuService menus) {
        this.menus = menus;
    }

    @GetMapping
    public List<Outlet> outlets() {
        return menus.outlets();
    }

    /** Without {@code reservationId} (walk-in) every verdict is {@code UNKNOWN}. */
    @GetMapping("/{rvc}/menu")
    public List<MenuItem> menu(@PathVariable int rvc, @RequestParam(required = false) String reservationId) {
        return menus.menu(rvc, reservationId);
    }
}
