import { Outlet } from 'react-router-dom';
import { useSessionRestore } from '../../hooks/useSessionRestore';

export default function RootLayout() {
  const isRestoring = useSessionRestore();

  // Hold rendering until we know whether the stored refresh token is valid.
  // Prevents a flash of the login page for authenticated users on refresh.
  if (isRestoring) return null;

  return <Outlet />;
}
