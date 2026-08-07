import React from 'react';
import { Navigate } from 'react-router-dom';

/** Ancienne route stub : le CRUD se fait sur la page liste. */
export default function CreateParishPage() {
  return <Navigate to="/admin/paroisses" replace />;
}
