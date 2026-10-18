import { Grid, Paper, Text, Title } from '@mantine/core'
import { useState } from 'react'
import { ArrivalsPanel } from '../components/ArrivalsPanel'
import { PassportEditor } from '../components/PassportEditor'

/** Front desk: arrivals worklist and the family allergy passport, a card per member (FE-B2). */
export function FrontDeskPage() {
  const [reservationId, setReservationId] = useState<string | null>(null)
  return (
    <Grid data-testid="front-desk-page" gap="lg">
      <Grid.Col span={{ base: 12, md: 5 }}>
        <Title order={2} mb="sm">
          Front desk — allergy passport
        </Title>
        <ArrivalsPanel selectedId={reservationId} onSelect={setReservationId} />
      </Grid.Col>
      <Grid.Col span={{ base: 12, md: 7 }}>
        {reservationId ? (
          <PassportEditor key={reservationId} reservationId={reservationId} />
        ) : (
          <Paper withBorder p="xl" data-testid="front-desk-passport-empty">
            <Text c="dimmed" ta="center">
              Choose an arrival or search by room or last name to record the family’s allergies.
            </Text>
          </Paper>
        )}
      </Grid.Col>
    </Grid>
  )
}
