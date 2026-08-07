import React from 'react';

export default function AppInput({ className = '', ...props }) {
  return <input className={`input ${className}`.trim()} {...props} />;
}
