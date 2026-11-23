import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'

import { AppLayout } from '@/components/layout/AppLayout'
import { ProtectedRoute } from '@/components/routing/ProtectedRoute'
import { GuestRoute } from '@/components/routing/GuestRoute'
import { HomePage } from '@/pages/HomePage'
import { LoginPage } from '@/pages/LoginPage'
import { RegisterPage } from '@/pages/RegisterPage'
import { ForgotPasswordPage } from '@/pages/ForgotPasswordPage'
import { ResetPasswordPage } from '@/pages/ResetPasswordPage'
import { ConfirmationPage } from '@/pages/ConfirmationPage'
import { ProfilePage } from '@/pages/ProfilePage'
import { ExercisesPage } from '@/pages/ExercisesPage'
import { PlansPage } from '@/pages/PlansPage'
import { PlanDetailsPage } from '@/pages/PlanDetailsPage'
import { PlanCreatePage } from '@/pages/PlanCreatePage'
import { PlanEditPage } from '@/pages/PlanEditPage'
import { WorkoutSessionPage } from '@/pages/WorkoutSessionPage'
import { ProgressPage } from '@/pages/ProgressPage'
import { CollaborationPage } from '@/pages/CollaborationPage'
import { TrainerPage } from '@/pages/TrainerPage'
import { TrainerRoute } from '@/components/routing/TrainerRoute'
import { AdminRoute } from '@/components/routing/AdminRoute'
import { AdminPage } from '@/pages/AdminPage'
import { CalendarPage } from '@/pages/CalendarPage'
import { StatsPage } from '@/pages/StatsPage'
import { CalculatorsPage } from '@/pages/CalculatorsPage'
import { SharedPlanPage } from '@/pages/SharedPlanPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/" element={<HomePage />} />
          <Route element={<ProtectedRoute />}>
            <Route path="/profil" element={<ProfilePage />} />
            <Route path="/cwiczenia" element={<ExercisesPage />} />
            <Route path="/plany" element={<PlansPage />} />
            <Route path="/plany/nowy" element={<PlanCreatePage />} />
            <Route path="/plany/:id/edytuj" element={<PlanEditPage />} />
            <Route path="/plany/:id" element={<PlanDetailsPage />} />
            <Route path="/trening" element={<WorkoutSessionPage />} />
            <Route path="/progres" element={<ProgressPage />} />
            <Route path="/kalendarz" element={<CalendarPage />} />
            <Route path="/statystyki" element={<StatsPage />} />
            <Route path="/kalkulatory" element={<CalculatorsPage />} />
            <Route path="/udostepnione/:token" element={<SharedPlanPage />} />
            <Route path="/wspolpraca" element={<CollaborationPage />} />
            <Route element={<TrainerRoute />}>
              <Route path="/trener" element={<TrainerPage />} />
            </Route>
            <Route element={<AdminRoute />}>
              <Route path="/admin" element={<AdminPage />} />
            </Route>
          </Route>
          <Route element={<GuestRoute />}>
            <Route path="/logowanie" element={<LoginPage />} />
            <Route path="/rejestracja" element={<RegisterPage />} />
          </Route>
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/confirm-email" element={<ConfirmationPage key="email" kind="email" />} />
          <Route path="/confirm-deletion" element={<ConfirmationPage key="deletion" kind="deletion" />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
