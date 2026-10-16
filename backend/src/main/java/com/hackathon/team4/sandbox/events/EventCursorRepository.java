package com.hackathon.team4.sandbox.events;

import org.springframework.data.jpa.repository.JpaRepository;

interface EventCursorRepository extends JpaRepository<EventCursor, String> {
}
