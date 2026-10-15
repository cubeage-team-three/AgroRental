import { Navigate } from 'react-router-dom';

function OperatorAuthChoice() {
  return <Navigate to="/register?role=operator" replace />;
}

export default OperatorAuthChoice;
