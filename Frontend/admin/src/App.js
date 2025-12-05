import './App.css';
import { Route, Routes } from 'react-router-dom';
import Signin from './pages/auth/Signin';
import PasswordRecovery from './pages/auth/PasswordRecovery';
import AuthLayout from './layouts/AuthLayout';
import DashboardLayout from './layouts/DashboardLayout';
import Board from './pages/dashboard/Board';
import WeeklyMasses from './pages/dashboard/massManagement/WeeklyMasses';
import SundayMasses from './pages/dashboard/massManagement/SundayMasses';
import SpecialMasses from './pages/dashboard/massManagement/SpecialMasses';

function App() {
  return (
    <Routes>

      <Route path="/" element={<AuthLayout />}>
        <Route path="/" element={<Signin />} />
        <Route path="passwordrecovery" element={<PasswordRecovery />} />
      </Route>

      <Route path="/dashboard/" element={
        <DashboardLayout />
      }>

        <Route path="/dashboard/" element={<Board />} />


        <Route path="messes/hebdomadaires/" element={<WeeklyMasses />} />
        <Route path="messes/dominicales/" element={<SundayMasses />} />
        <Route path="messes/spéciales/" element={<SpecialMasses />} />

      </Route>

    </Routes >
  );
}

export default App;