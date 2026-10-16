package com.hackathon.team4.sandbox.events;

import org.springframework.data.jpa.repository.JpaRepository;

interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
